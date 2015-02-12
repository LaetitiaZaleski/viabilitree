package fr.iscpif.lake

import fr.iscpif.model.Control
import fr.iscpif.viability._
import fr.iscpif.kdtree.algorithm._
import fr.iscpif.kdtree.structure._
import fr.iscpif.viability.kernel._
import fr.iscpif.kdtree.export._
import scala.util.Random
import scalax.io.Resource

/**
 * Created by ia on 15/12/2014.
 */

object LakeViabilityControlTest extends App {

  implicit val rng = new Random(42)

  val lake = new LakeViability with ZoneK {
/*
    override def controls = for {
        u1 <- -0.09 to 0.09 by 0.1
        u2 <- -0.09 to 0.09 by 0.1
      } yield Control(u1, u2)
*/
      override def depth = 12
      override def domain = Seq((0.0, 1.0), (0.0, 1.5))
  }
  val viabilityKernel = lake().last
  println ("fin calcul noyau ")
  val output = s"/tmp/lakeAnalysis${lake.depth}/"
  saveVTK2D(viabilityKernel, s"${output}testD${lake.depth}.vtk")

//  val file = Resource.fromFile(s"/tmp/lakeControlTest/traj")
  val point = Seq(0.2,0.8)
  //def u(p:Point):Point = Seq(0.0)
 /* def u(p:Point):Point = {
    val controlLeaf = viabilityKernel.containingLeaf(point)
    controlLeaf match {
      case None => throw new RuntimeException("No leaf containing the point")
      case Some(leaf) => leaf.content.control.getOrElse(0)
            }
    // normallement il faudrait écrire ici : model ou dynamic et pas l'exemple
    lake.controls(controlLeaf)
  }
*/
  // lake.traceTraj(point, u, 10, file)

  val controlLeaf = viabilityKernel.containingLeaf(point)
  val uIndex = controlLeaf match {
    case None => throw new RuntimeException("No leaf containing the point")
    case Some(leaf) => leaf.content.control.getOrElse(0)
  }
  val uValue = lake.controls(uIndex)
  val controlValue= uValue(point)
  print ("control ")
  println(controlValue)
  // ça va pas c'est censé être un point
  val traj = lake.trajectory(point,uValue,2)
  println(traj)

}


object LakeViabilityErodedAnalysis00 extends App {

  implicit val rng = new Random(42)

  val lake = new LakeViability with ZoneK {
    override def depth = 16

    override def domain = Seq((0.0, 1.0), (0.0, 1.5))

  }

  val output = s"/tmp/lakeAnalysis0/"
  val viabilityKernel = lake().last
  //  viabilityKernel.saveVTK2D(Resource.fromFile(s"${output}originalD${lake.depth}.vtk"))
  println("erosion 1")

  // pb condition d'execution de erodeInDomain(viabilityKernel, step)
  // que suppose learnboundary ? est-ce qu'il ne faut pas un point à true ?
    var eroded0 = lake.erodeInDomain(viabilityKernel, 40)
    saveVTK2D(eroded0, s"${output}/erodedD${lake.depth}PAS${40}.vtk")
    var eroded = eroded0
    var lake0  =
         new ViabilityKernel
         with LakeViability
           with LearnK {
          def k(p: Point) = eroded.label(p)
          override def domain = lake.domain
        }
      var viabilityKernel0 = lake0().lastWithTrace
      saveVTK2D(viabilityKernel0, s"${output}viabErodedD${lake.depth}PAS40.vtk")
  println("erosion boucle")

  for (step <- 1 to 8) {
      print("step ")
      println(step)
    eroded = lake.erodeInDomain(eroded0, 1)
    saveVTK2D(eroded, s"${output}/erodedD${lake.depth}PAS41.vtk")
      val lake0  =
        new ViabilityKernel
          with LakeViability
          with LearnK {
          def k(p: Point) = eroded.label(p)
          override def domain = lake.domain
        }

      val viabilityKernel0 = lake0().lastWithTrace
      saveVTK2D(viabilityKernel0, s"${output}viabErodedD${lake.depth}PAS41+${step}.vtk")
      println("erosion next")

      eroded0 = eroded
  }
}

object LakeViabilityErodedTest extends App {

  implicit val rng = new Random(42)

  val lake = new LakeViability with ZoneK {
    override def depth = 12
    override def domain = Seq((0.0, 1.0), (0.0, 1.5))
  }

 // val output = s"/tmp/lakeAnalysis${lake.depth}/"

  val output = s"/tmp/lakeErodeTest/"
  val viabilityKernel = lake().last
 //  viabilityKernel.saveVTK2D(Resource.fromFile(s"${output}originalD${lake.depth}.vtk"))

  val eroded1 = lake.erodeInDomain(viabilityKernel, 1)

//  val dilate1 = lake.dilate(viabilityKernel, 1)
  saveVTK2D(eroded1, s"${output}eroded1D${lake.depth}.vtk")
//  dilate1.saveVTK2D(Resource.fromFile(s"${output}dilate1D${lake.depth}.vtk"))
//  val erodedilate = lake.erode(dilate1, 1)
//  if (viabilityKernel.volume == eroded1.volume) println("même volume initial et erode dilate")
  println("erosion 2")
//  val eroded2 = lake.erode(eroded1, 1)
//  eroded2.saveVTK2D(Resource.fromFile(s"${output}eroded2D${lake.depth}.vtk"))
//  println("erosion 2 Direct")
//  val eroded2direct = lake.erode(viabilityKernel, 2)
//  if (eroded2direct.volume == eroded2.volume) println("même volume direct 2 et 2*1")
//  eroded2direct.saveVTK2D(Resource.fromFile(s"${output}eroded2Direct${lake.depth}.vtk"))

}

object LakeViabilityErodedAnalysis1 extends App {

  implicit val rng = new Random(42)

  val lake = new LakeViability with ZoneK {
    override def depth = 16
    override def domain = Seq((0.0, 1.0), (0.0, 1.5))

  }

  val output = s"/tmp/lakeErodeAnalysis/"
  val viabilityKernel = lake().last
  saveVTK2D(viabilityKernel, s"${output}originalD${lake.depth}.vtk")
  println("erosion 1")

  val eroded = lake.erodeInDomain(viabilityKernel, 2)
  saveVTK2D(eroded, s"${output}erodedD${lake.depth}.vtk")

  val lake2  =
    new ViabilityKernel
      with LakeViability
      with LearnK {
      def k(p: Point) = eroded.label(p)
      override def domain = lake.domain
  }

    val viabilityKernel2 = lake2().lastWithTrace
   saveVTK2D(viabilityKernel2, s"${output}viabErodedD${lake.depth}.vtk")
  println("erosion 2")
  val eroded2 = lake2.erodeInDomain(viabilityKernel2, 2)
  saveVTK2D(eroded2, s"${output}erodedViabErodedD${lake.depth}.vtk")

  val lake3  =
    new ViabilityKernel
      with LakeViability
      with LearnK {
      def k(p: Point) = eroded2.label(p)
      override def domain = lake.domain
    }

  val viabilityKernel3 = lake3().lastWithTrace
  saveVTK2D(viabilityKernel3, s"${output}viab2Eroded2D${lake.depth}.vtk")

  println("erosion 3")
  val eroded3 = lake3.erodeInDomain(viabilityKernel3, 2)
  saveVTK2D(eroded3, s"${output}erodedViab2Eroded2D${lake.depth}.vtk")

  val lake4  =
    new ViabilityKernel
      with LakeViability
      with LearnK {
      def k(p: Point) = eroded3.label(p)
      override def domain = lake.domain
    }

  val viabilityKernel4 = lake4().lastWithTrace
  saveVTK2D(viabilityKernel4, s"${output}viab3Eroded3D${lake.depth}.vtk")

  println("erosion 4")
  val eroded4 = lake4.erodeInDomain(viabilityKernel4, 2)
  saveVTK2D(eroded4, s"${output}erodedViab3Eroded3D${lake.depth}.vtk")

  val lake5  =
    new ViabilityKernel
      with LakeViability
      with LearnK {
      def k(p: Point) = eroded4.label(p)
      override def domain = lake.domain
    }

  val viabilityKernel5 = lake5().lastWithTrace
  saveVTK2D(viabilityKernel5, s"${output}viab4Eroded4D${lake.depth}.vtk")

  println("erosion 5")
  val eroded5 = lake5.erodeInDomain(viabilityKernel5, 2)
  saveVTK2D(eroded5, s"${output}erodedViab4Eroded4D${lake.depth}.vtk")

  val lake6  =
    new ViabilityKernel
      with LakeViability
      with LearnK {
      def k(p: Point) = eroded5.label(p)
      override def domain = lake.domain
    }

  val viabilityKernel6 = lake6().lastWithTrace
  saveVTK2D(viabilityKernel6, s"${output}viab5Eroded5D${lake.depth}.vtk")


}

object LakeViabilityErodedAnalysis0 extends App {

  implicit val rng = new Random(42)

  val lake = new LakeViability with ZoneK {
    override def depth = 16
    override def domain = Seq((0.0, 1.0), (0.0, 1.5))

  }

  val output = s"/tmp/lakeAnalysis0/"
  val viabilityKernel = lake().last
//  viabilityKernel.saveVTK2D(Resource.fromFile(s"${output}originalD${lake.depth}.vtk"))
  println("erosion 1")

  val step = 40
  val eroded = lake.erodeInDomain(viabilityKernel, step)
  saveVTK2D(eroded, s"${output}/erodedD${lake.depth}PAS${step}.vtk")

  val lakeViabilityAnalyse =
    new ViabilityKernel
      with LakeViability
      with LearnK {
      def k(p: Point) = eroded.label(p)
      override def domain = lake.domain
    }

  val viabilityEroded = lakeViabilityAnalyse().lastWithTrace
  saveVTK2D(viabilityEroded, s"${output}viabErodedD${lake.depth}PAS${step}.vtk")


  /*
    for {
      (k,s) <- lakeViabilityAnalyse().zipWithIndex
    } {
      println(s)
      k.saveVTK2D(Resource.fromFile(s"${output}/depth${lake.depth}/mu${lake.dilations}s$s.vtk"))
    }
  */

}

object LakeViabilityErodedAnalysis2 extends App {

  implicit val rng = new Random(42)

  val lake = new LakeViability with ZoneK {
    override def depth = 18
  }

  val nbErosion = 10
  val viabilityKernel = lake().lastWithTrace
  val eroded = lake.erodeInDomain(viabilityKernel, nbErosion)

  val lakeViabilityAnalyse =
    new ViabilityKernel
      with LakeViability {
      def tree0(implicit rng: Random) = Some(eroded)
      override def depth = 18
      override def domain = lake.domain
    }

  val output = s"/tmp/lakeAnalysis${lake.depth}/"

  saveVTK2D(viabilityKernel, s"${output}/viab${lake.dilations}.vtk")

  saveVTK2D(eroded, s"${output}/eroded${lake.dilations}.vtk")

  for {
    (k,s) <- lakeViabilityAnalyse().zipWithIndex
  } {
    println(s)
    saveVTK2D(k, s"${output}/nb${nbErosion}s$s.vtk")
  }

}
