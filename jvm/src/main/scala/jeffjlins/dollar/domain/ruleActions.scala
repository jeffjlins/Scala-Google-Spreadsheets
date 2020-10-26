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
    targets.map(t => actions.foldLeft(t)((tt, a) => a.operation(tt)))
  }
}

case class TransQualifyRuleAction(rule: Rule, group: String, actions: List[TransQualifyAction])
