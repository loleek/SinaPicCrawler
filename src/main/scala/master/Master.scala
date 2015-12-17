package master

import akka.actor.Actor
import akka.actor.ActorRef
import util.Messages.Register
import util.Messages.Unregister
import util.Messages.Shutdown
import util.Messages.Stop
import util.Messages.MasterInfo
import akka.actor.PoisonPill
import akka.actor.Props
import util.Messages.MasterInfo
/*
 * 管理控制节点
 */
class Master extends Actor {

  //保存主机名和ActorRef区分不同的slave
  var hosts = collection.mutable.HashMap.empty[String, ActorRef]
  //图片id管理器
  val pidManager = context.actorOf(Props[PidManager], "pidManager")
  //图片收集器
  val dataRecorder = context.actorOf(Props[DataRecorder], "dataRecorder")

  def receive = {
    //收到slvae注册消息将其actorref和主机名加入map
    case Register(hostname, ref) => {
      hosts += hostname -> ref
      sender ! MasterInfo(pidManager, dataRecorder)
    }
    //收到slave反注册消息，从map中删除该主机信息，如果map为空则关闭系统
    case Unregister(hostname) => {
      hosts -= hostname
      if (hosts.isEmpty){
        self ! PoisonPill
        context.system.shutdown()
      }
    }
    //主动关闭系统
    case Shutdown => {
      hosts.values.foreach { ref => ref ! Stop }
    }
  }
}