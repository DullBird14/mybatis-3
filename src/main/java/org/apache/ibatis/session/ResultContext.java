/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.session;

/**
 * @author Clinton Begin
 */
public interface ResultContext<T> {
  /**
   * 当前结果对象
   * @return
   */
  T getResultObject();

  /**
   * 结果数量
   * @return
   */
  int getResultCount();

  /**
   * 是否暂停 todo 不知道干嘛的
   * @return
   */
  boolean isStopped();

  /**
   * 暂定
   */
  void stop();

}