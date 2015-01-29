/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.iscpif.model

import org.apache.commons.math3.ode._
import org.apache.commons.math3.ode.nonstiff._

object Dynamic {
  def apply(equations: (Array[Double], Double) => Double*) = new Dynamic(equations: _*)
}

class Dynamic(equations: (Array[Double], Double) => Double*) extends FirstOrderDifferentialEquations {

  def integrate(y0: Array[Double], integrationStep: Double, step: Double): Seq[Double] =
    integrate(y0, integrationStep, Seq(0.0, step)).last._2.toSeq

  def integrate(y0: Array[Double], integrationStep: Double, samples: Seq[Double]) = {
    val integrator = new ClassicalRungeKuttaIntegrator(integrationStep)

    samples.tail.foldLeft((samples.head, y0) :: Nil) {
      case (ys, s) => {
        val (curT, curY) = ys.head
        val y = Array.ofDim[Double](equations.size)
        integrator.integrate(this, curT, curY, s, y)
        (s, y) :: ys
      }
    }.reverse
  }

  override def computeDerivatives(t: Double, y: Array[Double], yDot: Array[Double]): Unit =
    equations.zipWithIndex.foreach { case (eq, i) => yDot(i) = eq(y, t) }

  override def getDimension: Int = equations.size
}
