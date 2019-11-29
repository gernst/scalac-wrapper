ScalaC Wrapper
===============

A simple wrapper to parse and typecheck Scala source files:

    ScalaC.parse(files, classpath, hooks = Hooks.default)

Returns a list of compilation units, which contain typechecked `scala.reflect.api.Trees#Tree`s.
