package scife.util

// NOTE can be solved with a functional solution (stream of variables, not so pretty)
class FreshNameGenerator(initialString: String) {
  private var counter = 0
  // returns a string for a fresh variable name
  def getFreshVariableName: String = initialString + { counter += 1; counter }
}
