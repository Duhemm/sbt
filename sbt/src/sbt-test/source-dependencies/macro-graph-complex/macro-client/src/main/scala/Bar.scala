package macros

// If we don't add this class and something breaks (that is, if Client.scala is not
// recompiled after changes are made to Helper.scala), then there won't be any
// recompilation in this subproject, therefore Client.scala will have been part of the
// last compilation. This class is always recompiled if a method is added to Helper.scala,
// so we will be able to know for sure if Client.scala was part of the last recompilation.
class Bar extends Helper