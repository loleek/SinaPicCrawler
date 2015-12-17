package core

import akka.actor.ActorSystem
import akka.actor.Props
import master.Master
import com.typesafe.config.ConfigFactory
import slave.Slave
import util.Messages.Start
/*
 * 程序入口函数，根据master和slave参数启动不同的ActorSystem，
 * 配置文件中也使用master和slave区分不同配置
 */
object Main {
  def main(args: Array[String]): Unit = {
    args(0) match {
      case "master" => {
        val config = ConfigFactory.load()
        implicit val system = ActorSystem("SinaPictureCrawlerSystem", config.getConfig("master"))

        val master = system.actorOf(Props[Master],"master")
      }
      case "slave"=>{
        val config=ConfigFactory.load()
        val system=ActorSystem("SinaPictureCrawlerSystem", config.getConfig("slave"))
        
        val slave=system.actorOf(Props[Slave],"slave")
        slave ! Start
      }
    }
  }
}