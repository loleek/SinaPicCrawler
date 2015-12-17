package slave

import akka.actor.Actor
import akka.actor.ActorRef
import util.Messages.MasterInfo
import util.Messages.NoTask
import util.Messages.PictureIDRequest
import util.Messages.PictureIDTask
import util.Messages.TaskFailed
import java.net.InetAddress
import util.Messages.Picture
import util.Messages.Stop
import org.apache.http.impl.client.HttpClients
import scala.util.Random
import org.apache.http.client.methods.HttpGet
import java.io.FileOutputStream
import java.io.File
import scala.collection.mutable.ArrayBuffer
import java.io.IOException
/*
 * 图片爬虫
 */
class Spider extends Actor {

  var pidManagerRef: Option[ActorRef] = None
  var dataRecorderRef: Option[ActorRef] = None

  //主机名
  val hostname = InetAddress.getLocalHost.getHostName
  //图片服务器存在4个主机，每次随机取一个访问
  val hostList = List("http://ww1.sinaimg.cn/mw1024/", "http://ww2.sinaimg.cn/mw1024/", "http://ww3.sinaimg.cn/mw1024/", "http://ww4.sinaimg.cn/mw1024/")

  val client = HttpClients.createDefault()

  //失败计数，若失败15次则自动关闭
  var failureCount = 0

  def receive = {
    case MasterInfo(pmr, drr) => {
      pidManagerRef = Some(pmr)
      dataRecorderRef = Some(drr)

      pmr ! PictureIDRequest(hostname)
    }
    case NoTask => {
      context.parent ! Stop
    }
    case PictureIDTask(pid) => {
      Thread.sleep(3000)

      try {
        //随机选择图片服务器
        val num = Random.nextInt(4)
        //拼接url
        val url = s"${hostList(num)}${pid}"
        val request = new HttpGet(url)
        val response = client.execute(request)

        //取得图片类型
        val typefield = response.getFirstHeader("Content-Type").getValue
        val pictype = typefield.substring(typefield.indexOf('/') + 1) match {
          case "jpeg"=>"jpg"
          case any=>any
        }

        //图片数据缓存
        val data = ArrayBuffer.empty[Byte]
        val in = response.getEntity.getContent
        //图片分段数据缓存
        val databuffer = new Array[Byte](8192)
        //循环读取全部图片数据
        var templen = in.read(databuffer)
        while (templen > 0) {
          //因为分段缓存为8K但是每次不一定会全部写满，因此需要对分段缓存进行切片
          val dataseg = databuffer.slice(0, templen)
          //缓存分段数据
          data ++= dataseg
          templen = in.read(databuffer)
        }
        //将完整数据缓存发送给DataRecorder
        dataRecorderRef.get ! Picture(s"${pid}.${pictype}", data.toArray)
        //告知图片管理器抓取完成
        pidManagerRef.get ! PictureIDRequest(hostname)
      } catch {
        //发生异常时进行恢复，若超过恢复次数则关闭爬虫
        case _: IOException => {
          failureCount = failureCount + 1
          if (failureCount <= 15)
            pidManagerRef.get ! TaskFailed(hostname, pid, false)
          else {
            pidManagerRef.get ! TaskFailed(hostname, pid, true)
            context.parent ! Stop
          }
        }
      }
    }
  }
}