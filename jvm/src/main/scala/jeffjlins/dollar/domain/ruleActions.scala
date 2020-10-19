package jeffjlins.dollar.domain

trait RuleAction {
  val rule: Rule
  val group: String
  val actions: List[Action]
  def matchingSet(trans: List[Transaction]): List[Transaction] = {
    trans.filter(rule.matches)
  }
}

case class TransMutateRuleAction(rule: Rule, group: String, actions: List[TransMutateAction]) {
  def changeSet(trans: List[Transaction]): List[Transaction] = {
    val targets = trans.filter(rule.matches)
    actions.flatMap(a => targets.map(a.operation))
  }
}

case class TransQualifyRuleAction(rule: Rule, group: String, actions: List[TransQualifyAction])
