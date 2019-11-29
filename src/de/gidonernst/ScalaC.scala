package de.gidonernst

import scala.tools.nsc._
import scala.tools.nsc.reporters._
import scala.reflect.internal.util.Position
import scala.reflect.api.Trees

trait Hooks {
  def info(pos: Position, msg: String, force: Boolean): Unit
  def warning(pos: Position, msg: String, force: Boolean): Unit
  def error(pos: Position, msg: String, force: Boolean): Unit
  def progress(current: Int, total: Int): Unit
  def informUnitStarting(phase: String, unit: String)
}

object Hooks {
  object default extends Hooks {
    def info(pos: Position, msg: String, force: Boolean) { println("info: " + msg) }
    def warning(pos: Position, msg: String, force: Boolean) { println("warning: " + msg) }
    def error(pos: Position, msg: String, force: Boolean) { println("error: " + msg) }
    def progress(current: Int, total: Int) {}
    def informUnitStarting(phase: String, unit: String) {}
  }
}

case class CompilationUnit(file: String, tree: Trees#Tree)

object ScalaC {
  def scalaLib = {
    val source = scala.Predef.getClass.getProtectionDomain.getCodeSource
    assert(source != null, "No Scala library found.")
    source.getLocation.getPath
  }

  def parse(filename: String, classpath: String, hooks: Hooks): CompilationUnit = {
    val filenames = List(filename)
    val units = parse(filenames, classpath, hooks)
    val List(unit) = units
    unit
  }

  def parse(filenames: List[String], classpath: String, hooks: Hooks): List[CompilationUnit] = {
    val _classpath = classpath

    object settings extends Settings {
      classpath.value = _classpath
      // usejavacp.value = true

      feature.value = true
      unchecked.value = true
      deprecation.value = true

      Yrangepos.value = true
    }

    object reporter extends Reporter {
      def info0(pos: Position, msg: String, severity: Severity, force: Boolean) = severity match {
        case INFO =>
          hooks.info(pos, msg, force)
        case WARNING =>
          hooks.warning(pos, msg, force)
        case ERROR =>
          hooks.error(pos, msg, force)
      }
    }

    object global extends Global(settings, reporter) {
      override protected def computeInternalPhases(): Unit = {
        addToPhasesSet(syntaxAnalyzer, "parse source into ASTs, perform simple desugaring")
        addToPhasesSet(analyzer.namerFactory, "resolve names, attach symbols to named trees")
        addToPhasesSet(analyzer.packageObjects, "load package objects")
        addToPhasesSet(analyzer.typerFactory, "the meat and potatoes: type the trees")
      }

    }

    object run extends global.Run {
      override def progress(current: Int, total: Int) {
        hooks.progress(current, total)
      }

      override def informUnitStarting(phase: Phase, unit: global.CompilationUnit) {
        hooks.informUnitStarting(phase.name, unit.source.file.name)
      }
    }

    run.compile(filenames)

    for (unit <- run.units.toList) yield {
      CompilationUnit(unit.source.file.path, unit.body)
    }
  }
}