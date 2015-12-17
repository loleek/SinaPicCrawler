package util

import akka.actor.ActorRef
/*
 * 所有消息均定义在Messages中
 */
object Messages {
  /*
   * 用于Master和Slave间通信的消息
   */
  //注册
  case class Register(hostname: String, slaveref: ActorRef)
  //反注册
  case class Unregister(hostname: String)
  //主节点信息（包括图片id管理器引用，图片收集器引用）
  case class MasterInfo(pidManagerRef: ActorRef, dataRecorderRef: ActorRef)
  //关闭系统
  case object Shutdown
  //开启slave
  case object Start
  //关闭slave
  case object Stop

  /*
   * 用于Spider与PidManager通信的消息
   */
  //请求图片ID
  case class PictureIDRequest(hostname: String)
  //回送图片ID
  case class PictureIDTask(pid: String)
  //错误图片ID
  case class TaskFailed(hostname: String, pid: String, flag: Boolean)
  //抓取队列为空
  case object NoTask
  /*
   * 用于Spider与DataRecorder通信的消息
   * 所有图片数据都保存在Byte数组中，图片以id命名，目前已知图片类型包括(jpg,gif)
   */
  case class Picture(name: String, data: Array[Byte])

}