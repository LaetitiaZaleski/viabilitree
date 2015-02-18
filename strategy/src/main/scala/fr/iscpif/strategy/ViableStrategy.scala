package fr.iscpif.strategy

import fr.iscpif.geometry._
import fr.iscpif.kdtree.structure.{Leaf, Tree}
import fr.iscpif.model.Control
import fr.iscpif.viability.control.ControlledDynamicContent

/**
 * Created by ia on 18/02/2015.
 */
//TODO should extend strategy
// if works should remouve other attempt of ControlStrategy in package model or viability

 object ViableStrategy {
  def apply(viab: Tree[ControlledDynamicContent.Content], set: Seq[Control] ) = new ViableStrategy {
    def viability = viab
    def controlSet = set
    def apply(x:Point): Control = viableStrategy(x)
  }
}

trait ViableStrategy {
  def viability : Tree[ControlledDynamicContent.Content]
  def controlSet : Seq[Control]

   def viableStrategy (p: Point): Control = {
    val leafP = viability.containingLeaf(p)
    val labelP: Boolean = leafP match {
      case None => false
      case Some(leaf) => leaf.content.label
    }
    val controlValue = {
      if (labelP) {
        val uIndex : Int = leafP match {
          case None => throw new RuntimeException("No leaf containing the point")
          case Some(leaf: Leaf[ControlledDynamicContent.Content]) => leaf.content.control match {
            case None => throw new RuntimeException("Viable leaves must have a control")
            case Some(int) => int
          }
        }
        controlSet(uIndex)
      } else {
        throw new RuntimeException("NOT viable")
      }
    }
    controlValue}
}
