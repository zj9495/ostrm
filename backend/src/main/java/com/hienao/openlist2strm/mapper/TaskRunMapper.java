package com.hienao.openlist2strm.mapper;

import com.hienao.openlist2strm.entity.TaskRun;
import com.hienao.openlist2strm.entity.TaskRunLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 任务运行记录Mapper接口 */
@Mapper
public interface TaskRunMapper {

  /**
   * 插入任务运行记录
   *
   * @param taskRun 任务运行记录
   * @return 影响行数
   */
  int insertRun(TaskRun taskRun);

  /**
   * 根据ID查询任务运行记录
   *
   * @param id 主键ID
   * @return 任务运行记录
   */
  TaskRun selectRunById(@Param("id") Long id);

  /**
   * 查询指定任务的运行记录
   *
   * @param taskConfigId 任务配置ID
   * @return 运行记录列表
   */
  List<TaskRun> selectRunsByTaskConfigId(@Param("taskConfigId") Long taskConfigId);

  /**
   * 更新任务运行状态
   *
   * @param taskRun 任务运行记录
   * @return 影响行数
   */
  int updateRunById(TaskRun taskRun);

  /**
   * 插入任务运行日志
   *
   * @param taskRunLog 任务运行日志
   * @return 影响行数
   */
  int insertRunLog(TaskRunLog taskRunLog);

  /**
   * 查询指定运行记录的日志
   *
   * @param taskRunId 运行记录ID
   * @return 运行日志列表
   */
  List<TaskRunLog> selectLogsByTaskRunId(@Param("taskRunId") Long taskRunId);
}
