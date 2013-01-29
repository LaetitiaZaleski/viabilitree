package fr.iscpif.viability

// TODO: Using bounded version, should we change to the unbounded one?

import fr.iscpif.viability.kdtreeBounded._
import fr.iscpif.viability.initialSteps.InputProcessing._
import fr.iscpif.viability.viabilityRoot._
import fr.iscpif.viability.initialSteps._
import fr.iscpif.viability.LanguageModel._

import scalax.io._
import scalax.file.Path
import scalax.io.StandardOpenOption._
import math._
import Function._
import scala.util.Random


package object viabilityLanguages {

  //TODO: Review!! And create the inputs properly (in a class)
  //INITIAL ARGUMENTS
  val stateDimension = 3
  val controlDimension = 1
  val maxDepth = 8
  val numberOfControlTests = 20
  val randomNG = new Random(3)
  val timeStep: Double = 1.0

  val integrationStep: Double = 0.1
  val sampleTimes: Seq[Double] = Seq(0.0, timeStep)
  val model = modelCreation(integrationStep, sampleTimes)


  //TODO: Should it be defined elsewhere?
  // TYPES
  type State = Array[Double]
  type Control = Double
  type Model = (State, Control) => State
  type IndicatorFunction = Array[Double] => Boolean
  type RichIndicatorFunction = Array[Double] => Option[Double]

  def conversionToIndicator(rIFunction: RichIndicatorFunction): IndicatorFunction = {
    x:Array[Double] =>
      rIFunction(x) match {
        case Some(_) => true
        case None => false
      }
  }

  // TODO: Change [epsilon]
  // Since sDot = uControl(t) and s should belong to [0,1], u must be in the interval defined below. [timeStep] is the
  // time interval between two slices. [epsilon] is used to avoid numerical problems: analytically we'll get s\in [0+epsilon, 1-epsilon]
  def validControlInterval(state: State) = {
    val epsilon = pow(10, -10)
    new Interval((epsilon-state.s)/timeStep, (1-state.s-epsilon)/timeStep)
  }

  def controlTestOrder(interval: Interval): Array[Double] = {
    val step = (interval.max - interval.min) / (numberOfControlTests - 1)
    val controlTests = (interval.min to interval.max by step)
    def reordering(x: Seq[Double], accumulator: Seq[Double], side: Boolean): Array[Double] = {
      if (x == Nil) accumulator.toArray
      else if (side) reordering(x.drop(1), accumulator :+ x(0), false)
      else reordering(x.dropRight(1), accumulator :+ x.last, true)
    }
    reordering(controlTests, Nil, true)
  }

  def randomConstrainedPoint(implicit rng: Random): Point = {
    val sigmaA = rng.nextDouble()
    val sigmaB = rng.nextDouble()*(1-sigmaA)
    val s = rng.nextDouble()
    Array(sigmaA, sigmaB, s)
  }

  // TODO: We may be interested in monitoring the number of guesses
  def initialPointGuesser(targetIFunction: IndicatorFunction)(implicit rng: Random): (State, Control) = {

    var initialPointFound = false
    //The initialisation is meaningless, the values will be immediately overridden
    var guessPoint: State = null
    var guessControl: Double = 0.0
    while (!initialPointFound) {
      guessPoint = randomConstrainedPoint(rng)
      val guessControlArray = controlTestOrder(validControlInterval(guessPoint))
      var k = 0
      while(k< numberOfControlTests && !initialPointFound){
        guessControl = guessControlArray(k)
        val image = model(guessPoint, guessControl)
        k += 1
        //TODO: Delete. Debug
        println("Searching for an initial point..." +  " guessPoint = " + guessPoint.toList + " guessControl = " + guessControl)
        //val
        if (targetIFunction(image) == true) {
        //if (targetIFunction(image) == true) {
          initialPointFound = true
          //TODO: Delete. Debug
          println("Initial point found!" +  " guessPoint = " + guessPoint.toList + " guessControl = " + guessControl)
        }
        else println("targetIFunction(image) == FALSE")
      }
    }
    (guessPoint, guessControl)
  }

  def initialNodeCreation(richTargetIFunction: RichIndicatorFunction)(implicit rng: Random): Node = {
    val targetIFunction = conversionToIndicator(richTargetIFunction)
    val root = new Leaf {

      val stateControl:(State, Double) = initialPointGuesser(targetIFunction)(rng)
      val testPoint: Point = stateControl._1
      val control: Option[Double] = Some(stateControl._2)

      val zone = new Zone {
        val unitInterval = new Interval(0, 1)
        val region: Array[Interval] = Array(unitInterval, unitInterval, unitInterval)
      }
    }
    root
  }

  def targetIFunctionCreation(target: Node, model: Model): RichIndicatorFunction = {
    state: State => {
        if (state.sigmaA + state.sigmaB > 1) None
        else {
          var k = 0
          val guessControlArray = controlTestOrder(validControlInterval(state))
          var control: Option[Double] = None
          var controlFound = false
          while (k < numberOfControlTests && !controlFound) {
            val image = model(state, guessControlArray(k))
            if (target.isInKdTree(image) == true){control = Some(guessControlArray(k)) ; controlFound = true}
            k += 1
          }
          control
        }
      }

  }

  //The "slices" are computed dynamically
  def captureTube()(implicit rng: Random): Int => Node = {

    case 0 => throw new RuntimeException("The 0-slice of the capture tube is just the set of target points")
    case 1 => {
      val firstSlice = initialSteps.FirstKdTree.firstKdTree

      deleteFile("./OutputKdTrees/kdTree1.csv")
      val output: Output = Resource.fromFile("./OutputKdTrees/kdTree1.csv")
      kdTreeToFile(firstSlice, output)

      firstSlice
    }
    case n => {
      val firstSlice = initialSteps.FirstKdTree.firstKdTree

      deleteFile("./OutputKdTrees/kdTree1.csv")
      val output1: Output = Resource.fromFile("./OutputKdTrees/kdTree1.csv")
      kdTreeToFile(firstSlice, output1)

      def targetIFunction: RichIndicatorFunction = targetIFunctionCreation(firstSlice, model)
      val initNode = initialNodeCreation(targetIFunction)
      //Here the current slice is the second slice (the one that is two steps before the target)
      var currentSlice: Node = kdTreeComputation(initNode, maxDepth, targetIFunction)(rng)

      deleteFile("./OutputKdTrees/kdTree2.csv")
      val output2: Output = Resource.fromFile("./OutputKdTrees/kdTree2.csv")
      kdTreeToFile(firstSlice, output2)

      var i = 2
      while (i < n) {
        def currentIFunction: RichIndicatorFunction = targetIFunctionCreation(currentSlice, model)
        val initNode = initialNodeCreation(currentIFunction)
        currentSlice = kdTreeComputation(initNode, maxDepth, currentIFunction)(rng)

        val outputFile: String = "OutputKdTrees/kdTree" + (i+1).toString() +".csv"
        deleteFile(outputFile)
        val output: Output = Resource.fromFile(outputFile)
        kdTreeToFile(currentSlice, output)
        i += 1
      }
      currentSlice
    }
  }


  // OUTPUT FUNCTIONS

  def leafZoneToFile(leaf: Leaf, outputResource: Output) {
    //val output: Output = Resource.fromFile(outputFile)
    assert(leaf.zone.region.length == 3)
    val intervals = leaf.zone.region
    val origin = Array(intervals(0).min, intervals(1).min, intervals(2).min)
    val lengthSA = (intervals(0).max - intervals(0).min).toString()
    val lengthSB = (intervals(1).max - intervals(1).min).toString()
    val lengthS = (intervals(2).max - intervals(2).min).toString()
    val line: List[String] =
      List(origin(0).toString(), origin(1).toString(), origin(2).toString(), lengthSA, lengthSB, lengthS)
    //val list: List[String] = List(leaf.)
    outputResource.writeStrings(line,";")(Codec.UTF8)
    outputResource.write("\n")
  }

  def kdTreeToFile(node: Node, outputResource: Output) {
    node match {
      case leaf: Leaf => if(leaf.label == true){leafZoneToFile(leaf, outputResource) ; println("LEAF PRINTED" + leaf )}
      case fork: Fork => kdTreeToFile(fork.lowChild, outputResource) ; kdTreeToFile(fork.highChild, outputResource)
    }
  }

  def deleteFile(pathName: String){
     val path = scalax.file.Path.fromString(pathName)
     if(path.exists) path.delete(false)
  }


  //TEST SIMPLE FIGURES
  def iFunctionSphere():Array[Double] => Option[Double] = {
    point: Array[Double] =>{
      if (pow(point(0), 2) + pow(point(1), 2) + pow(point(2), 2) <= 1) Some(0.0)
      else None
    }
  }
  def iFunctionSmallCube():Array[Double] => Option[Double] = {
    point: Array[Double] =>{
      if (-0.5 <= point(0) && point(0) <= 0.5 && -0.5 <= point(1) && point(1) <= 0.5 && -0.5 <= point(2) && point(2) <= 0.5) Some(0.0)
      else None
    }
  }


  val root = new Leaf {

    val testPoint: Point = Array(0.0,0.0,0.0)
    val control: Option[Double] = Some(0.0)

    val zone = new Zone {
      val interval = new Interval(-1, 1)
      val region: Array[Interval] = Array(interval, interval, interval)
    }
  }

  def test(depth: Int) {
    val kdTreeSphere = kdTreeComputation(root, depth, iFunctionSmallCube())(randomNG)
    val output: Output = Resource.fromFile("./OutputKdTrees/kdTree_SmallCube_depth" + depth +".csv")
    kdTreeToFile(kdTreeSphere, output)
  }


  /// MAIN


  def main(args: Array[String]) {
    println("Hello, world!")
    test(5)

    //captureTube()(randomNG)(3)


    // Test output
    /*
    val leafTest: Leaf = new Leaf {
      val reversePath = Seq.empty
      val zone = new Zone {
        val unitInterval = new Interval(0, 1)
        val region: Array[Interval] = Array(unitInterval, unitInterval, unitInterval)
      }
      val testPoint =  Array(0.0,0.0,0.0)
      val control = Some(0.3)//None
    }
   val outputResource: Output = Resource.fromFile("LeafOutput.text")
   leafZoneToFile(leafTest, outputResource)
   */

    //deleteFile("LeafOutput.text")







  }


}