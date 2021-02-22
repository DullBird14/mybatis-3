/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor.resultset;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.UnknownTypeHandler;

/**
 * @author Iwao AVE!
 */
public class ResultSetWrapper {
  /**
   * ResultSet 对象
   */
  private final ResultSet resultSet;
  private final TypeHandlerRegistry typeHandlerRegistry;
  /**
   * 字段的名字的数组
   */
  private final List<String> columnNames = new ArrayList<String>();
  /**
   * 字段的 Java Type 的数组
   */
  private final List<String> classNames = new ArrayList<String>();
  /**
   * 字段的 JdbcType 的数组
   */
  private final List<JdbcType> jdbcTypes = new ArrayList<JdbcType>();
  /**
   * key1: 字段的名称
   * key2: java属性类型
   */
  private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<String, Map<Class<?>, TypeHandler<?>>>();
  /**
   * 有 mapped 的字段的名字的映射，
   *<resultMap id="B" type="Object">
   *     //这一段
   *     <result property="year" column="year"/>
   * </resultMap>
   * KEY：{@link #getMapKey(ResultMap, String)}
   * VALUE：字段的名字的数组
   */
  private final Map<String, List<String>> mappedColumnNamesMap = new HashMap<String, List<String>>();
  /**
   * 无 mapped 的字段的名字的映射
   *
   * 和 {@link #mappedColumnNamesMap} 相反
   */
  private final Map<String, List<String>> unMappedColumnNamesMap = new HashMap<String, List<String>>();

  public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {
    super();
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.resultSet = rs;
    final ResultSetMetaData metaData = rs.getMetaData();
    final int columnCount = metaData.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
      jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
      classNames.add(metaData.getColumnClassName(i));
    }
  }

  public ResultSet getResultSet() {
    return resultSet;
  }

  public List<String> getColumnNames() {
    return this.columnNames;
  }

  public List<String> getClassNames() {
    return Collections.unmodifiableList(classNames);
  }

  public JdbcType getJdbcType(String columnName) {
    // 通过名字匹配位置。然后取类型
    for (int i = 0 ; i < columnNames.size(); i++) {
      if (columnNames.get(i).equalsIgnoreCase(columnName)) {
        return jdbcTypes.get(i);
      }
    }
    return null;
  }

  /**
   * Gets the type handler to use when reading the result set.
   * Tries to get from the TypeHandlerRegistry by searching for the property type.
   * If not found it gets the column JDBC type and tries to get a handler for it.
   * 根据属性名称获取一个类型处理器
   * @param propertyType
   * @param columnName
   * @return
   */
  public TypeHandler<?> getTypeHandler(Class<?> propertyType, String columnName) {
    TypeHandler<?> handler = null;
    // 根据属性名称，取类型处理器
    Map<Class<?>, TypeHandler<?>> columnHandlers = typeHandlerMap.get(columnName);
    if (columnHandlers == null) {
      // 如果是第一次。肯定找不到。放入一个
      columnHandlers = new HashMap<Class<?>, TypeHandler<?>>();
      typeHandlerMap.put(columnName, columnHandlers);
    } else {
      // 如果已经找到了。根据类型去拿
      handler = columnHandlers.get(propertyType);
    }
    if (handler == null) {
      // 如果拿不到处理器，先根据列名称取到JDBC的类型
      JdbcType jdbcType = getJdbcType(columnName);
      // 然后从 typeHandlerRegistry 中寻找处理器
      handler = typeHandlerRegistry.getTypeHandler(propertyType, jdbcType);
      // Replicate logic of UnknownTypeHandler#resolveTypeHandler
      // See issue #59 comment 10
      if (handler == null || handler instanceof UnknownTypeHandler) {
        // 找不到处理器。再找
        final int index = columnNames.indexOf(columnName);
        final Class<?> javaType = resolveClass(classNames.get(index));
        if (javaType != null && jdbcType != null) {
          handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
        } else if (javaType != null) {
          handler = typeHandlerRegistry.getTypeHandler(javaType);
        } else if (jdbcType != null) {
          handler = typeHandlerRegistry.getTypeHandler(jdbcType);
        }
      }
      if (handler == null || handler instanceof UnknownTypeHandler) {
        // 如果还找不到。只能用默认的 ObjectTypeHandler处理
        handler = new ObjectTypeHandler();
      }
      columnHandlers.put(propertyType, handler);
    }
    return handler;
  }

  private Class<?> resolveClass(String className) {
    // 获取对应的类
    try {
      // #699 className could be null
      if (className != null) {
        return Resources.classForName(className);
      }
    } catch (ClassNotFoundException e) {
      // ignore
    }
    return null;
  }

  private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    List<String> mappedColumnNames = new ArrayList<String>();
    List<String> unmappedColumnNames = new ArrayList<String>();
    // 前缀大写
    final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
    // 处理前缀
    final Set<String> mappedColumns = prependPrefixes(resultMap.getMappedColumns(), upperColumnPrefix);
    for (String columnName : columnNames) {
      // 全部大写
      final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
      // 如果已经存在放到有mapped，没有就放到unmapped里面
      if (mappedColumns.contains(upperColumnName)) {
        mappedColumnNames.add(upperColumnName);
      } else {
        unmappedColumnNames.add(columnName);
      }
    }
    // 获取mapKey是 ResultMap.id + ":" + 前缀
    mappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), mappedColumnNames);
    unMappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), unmappedColumnNames);
  }

  public List<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    // 获取map数组
    List<String> mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    if (mappedColumnNames == null) {
      // 如果是空的调用初始化方法
      loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
      // 再获取
      mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    }
    return mappedColumnNames;
  }

  public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    //逻辑同上 org.apache.ibatis.executor.resultset.ResultSetWrapper.getMappedColumnNames
    List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    if (unMappedColumnNames == null) {
      loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
      unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    }
    return unMappedColumnNames;
  }

  private String getMapKey(ResultMap resultMap, String columnPrefix) {
    return resultMap.getId() + ":" + columnPrefix;
  }

  private Set<String> prependPrefixes(Set<String> columnNames, String prefix) {
    // 遍历增加前缀
    if (columnNames == null || columnNames.isEmpty() || prefix == null || prefix.length() == 0) {
      return columnNames;
    }
    final Set<String> prefixed = new HashSet<String>();
    for (String columnName : columnNames) {
      prefixed.add(prefix + columnName);
    }
    return prefixed;
  }

}

