package seek.aws.cloudformation

sealed trait StackStatus {
  def name: String
}

sealed trait CompleteStackStatus   extends StackStatus
sealed trait FailedStackStatus     extends StackStatus
sealed trait InProgressStackStatus extends StackStatus

case object CreateComplete         extends CompleteStackStatus { def name = "CREATE_COMPLETE" }
case object UpdateComplete         extends CompleteStackStatus { def name = "UPDATE_COMPLETE" }
case object RollbackComplete       extends CompleteStackStatus { def name = "ROLLBACK_COMPLETE" }
case object UpdateRollbackComplete extends CompleteStackStatus { def name = "UPDATE_ROLLBACK_COMPLETE" }
case object DeleteComplete         extends CompleteStackStatus { def name = "DELETE_COMPLETE" }

case object CreateFailed         extends FailedStackStatus { def name = "CREATE_FAILED" }
case object RollbackFailed       extends FailedStackStatus { def name = "ROLLBACK_FAILED" }
case object DeleteFailed         extends FailedStackStatus { def name = "DELETE_FAILED" }
case object UpdateRollbackFailed extends FailedStackStatus { def name = "UPDATE_ROLLBACK_FAILED" }

case object CreateInProgress                        extends InProgressStackStatus { def name = "CREATE_IN_PROGRESS" }
case object RollbackInProgress                      extends InProgressStackStatus { def name = "ROLLBACK_IN_PROGRESS" }
case object DeleteInProgress                        extends InProgressStackStatus { def name = "DELETE_IN_PROGRESS" }
case object UpdateInProgress                        extends InProgressStackStatus { def name = "UPDATE_IN_PROGRESS" }
case object UpdateCompleteCleanupInProgress         extends InProgressStackStatus { def name = "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS" }
case object UpdateRollbackInProgress                extends InProgressStackStatus { def name = "UPDATE_ROLLBACK_IN_PROGRESS" }
case object UpdateRollbackCompleteCleanupInProgress extends InProgressStackStatus { def name = "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS" }

object StackStatus {
  val AllStatuses = List(
    CreateComplete, UpdateComplete, RollbackComplete, UpdateRollbackComplete, DeleteComplete,
    CreateFailed, RollbackFailed, DeleteFailed, UpdateRollbackFailed,
    CreateInProgress, RollbackInProgress, DeleteInProgress, UpdateInProgress,
    UpdateCompleteCleanupInProgress, UpdateRollbackInProgress, UpdateRollbackCompleteCleanupInProgress)

  def apply(name: String): StackStatus =
    AllStatuses.find(_.name == name).getOrElse(throw new Exception(s"Unrecognised stack status ${name}"))
}
