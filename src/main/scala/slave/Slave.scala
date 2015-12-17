package slave

import akka.actor.Actor
import util.Messages.Start
import util.Messages.Register
import java.net.InetAddress
import util.Messages.Stop
import util.Messages.Unregister
import util.Messages.Unregister
import util.Messages.MasterInfo
import akka.actor.PoisonPill
import util.Messages.MasterInfo
import akka.actor.ActorRef
import akka.actor.Props
/*
 * 受控节点
 */
class Slave extends Actor {

  //主机名
  val hostname = InetAddress.getLocalHost.getHostName
  //管理节点引用
  val masterRef = context.actorSelection("akka.tcp://SinaPictureCrawlerSystem@192.168.1.126:5350/user/master")
  //图片爬虫
  val spider=context.actorOf(Props[Spider], "spider")

  def receive = {
    //启动时注册
    case Start => masterRef ! Register(hostname, self)
    //关闭时反注册
    case Stop => {
      masterRef ! Unregister(hostname)
      self ! PoisonPill
      context.system.shutdown()
    }
    //收到主节点相关组件消息时转发到爬虫
    case info @ MasterInfo(pmref, drref) => {
      spider ! info
    }
  }
}