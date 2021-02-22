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
package org.apache.ibatis.executor.keygen;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

  /**
   * 共享的单例
   * A shared instance.
   * @since 3.4.3
   */
  public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();

  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    // do nothing 什么也不做
  }

  @Override
  public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    processBatch(ms, stmt, getParameters(parameter));
  }

  public void processBatch(MappedStatement ms, Statement stmt, Collection<Object> parameters) {
    ResultSet rs = null;
    try {
      // 获取返回的自增主键
      rs = stmt.getGeneratedKeys();
      final Configuration configuration = ms.getConfiguration();
      final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
      // 获取自增主键的配置
      final String[] keyProperties = ms.getKeyProperties();
      final ResultSetMetaData rsmd = rs.getMetaData();
      TypeHandler<?>[] typeHandlers = null;
      if (keyProperties != null && rsmd.getColumnCount() >= keyProperties.length) {
        // 如果 配置不为null，并且配置的长度大于返回的结果长度
        for (Object parameter : parameters) {
          // there should be one row for each statement (also one for each parameter)
          if (!rs.next()) {
            // 如果没有返回的主键的情况
            break;
          }
          final MetaObject metaParam = configuration.newMetaObject(parameter);
          if (typeHandlers == null) {
            // 如果类型处理器没有，先获取类型处理器
            typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties, rsmd);
          }
          // 给 parameter 设置id
          populateKeys(rs, metaParam, keyProperties, typeHandlers);
        }
      }
    } catch (Exception e) {
      throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

  private Collection<Object> getParameters(Object parameter) {
    Collection<Object> parameters = null;
    if (parameter instanceof Collection) {
      parameters = (Collection) parameter;
    } else if (parameter instanceof Map) {
      Map parameterMap = (Map) parameter;
      if (parameterMap.containsKey("collection")) {
        parameters = (Collection) parameterMap.get("collection");
      } else if (parameterMap.containsKey("list")) {
        parameters = (List) parameterMap.get("list");
      } else if (parameterMap.containsKey("array")) {
        parameters = Arrays.asList((Object[]) parameterMap.get("array"));
      }
    }
    if (parameters == null) {
      parameters = new ArrayList<Object>();
      parameters.add(parameter);
    }
    return parameters;
  }

  private TypeHandler<?>[] getTypeHandlers(TypeHandlerRegistry typeHandlerRegistry, MetaObject metaParam, String[] keyProperties, ResultSetMetaData rsmd) throws SQLException {
    // 获得主键们，对应的每个属性的，对应的 TypeHandler 对象
    TypeHandler<?>[] typeHandlers = new TypeHandler<?>[keyProperties.length];
    for (int i = 0; i < keyProperties.length; i++) {
      // params 中是否有属性
      if (metaParam.hasSetter(keyProperties[i])) {
        TypeHandler<?> th;
        try {
          Class<?> keyPropertyType = metaParam.getSetterType(keyProperties[i]);
          // 获取类型处理器。
          th = typeHandlerRegistry.getTypeHandler(keyPropertyType, JdbcType.forCode(rsmd.getColumnType(i + 1)));
        } catch (BindingException e) {
          th = null;
        }
        typeHandlers[i] = th;
      }
    }
    return typeHandlers;
  }

  private void populateKeys(ResultSet rs, MetaObject metaParam, String[] keyProperties, TypeHandler<?>[] typeHandlers) throws SQLException {
    for (int i = 0; i < keyProperties.length; i++) {
      String property = keyProperties[i];
      TypeHandler<?> th = typeHandlers[i];
      if (th != null) {
        // 根据类型处理器获取
        Object value = th.getResult(rs, i + 1);
        metaParam.setValue(property, value);
      }
    }
  }

}