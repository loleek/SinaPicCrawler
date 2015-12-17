package master

import java.io.File
import java.io.FileOutputStream

import akka.actor.Actor
import util.Messages.Picture
/*
 * 图片收集器
 */
class DataRecorder extends Actor{
  def receive = {
    //保存收到的图片
    case Picture(name,data) =>{
      val file=new File("pictures"+File.separatorChar+name)
      val out=new FileOutputStream(file)
      out.write(data)
      out.flush()
      out.close()
    }
  }
}