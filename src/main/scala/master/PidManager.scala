package master

import java.io.File
import java.io.PrintWriter
import scala.collection.mutable.Queue
import scala.io.Source
import akka.actor.Actor
import util.Messages.PictureIDRequest
import util.Messages.PictureIDTask
import util.Messages.TaskFailed
import util.Messages.PictureIDTask
import util.Messages.TaskFailed
import util.Messages.NoTask
/*
 * 图片id管理器
 */
class PidManager extends Actor {
  //保存每个slave正在抓取的图片id
  var tasks = collection.mutable.HashMap.empty[String, String]
  //待抓取图片id队列
  val taskqueue = new Queue[String]()

  def receive = {
    //收到slave请求id后提取待抓取队列图片id，如果队列为空则告知slave队列为空
    case PictureIDRequest(hostname) => {
      if (!taskqueue.isEmpty) {
        val pid = taskqueue.dequeue()
        tasks += (hostname -> pid)
        sender ! PictureIDTask(pid)
      } else {
        sender ! NoTask
      }
    }
    //任务失败后将失败图片id存入待抓取队列
    case TaskFailed(hostname, pid, flag) => {
      taskqueue.enqueue(pid)
      if (!flag) {
        val newpid = taskqueue.dequeue()
        tasks += (hostname -> newpid)
        sender ! PictureIDTask(newpid)
      } else {
        tasks -= hostname
      }
    }
  }

  //启动时将文件中图片id全部载入
  override def preStart() {
    val file = new File("tasks" + File.separatorChar + "pictureids.txt")
    val lines = Source.fromFile(file).getLines()
    lines.foreach { taskqueue.enqueue(_) }
  }

  //系统关闭时将待抓取队列中所有图片id持久化到硬盘
  override def postStop() {
    val file = new File("tasks" + File.separatorChar + "remainedids.txt")
    if (file.exists()) file.delete()
    file.createNewFile()
    val out = new PrintWriter(file)
    taskqueue.toList.foreach { out.println(_) }
    out.flush()
    out.close()
  }
}