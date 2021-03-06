package scrum.client.tasks;

import ilarkesto.core.scope.Scope;
import ilarkesto.gwt.client.undo.AUndoOperation;
import scrum.client.admin.Auth;
import scrum.client.dnd.BlockListDropAction;
import scrum.client.project.Requirement;
import scrum.client.sprint.Task;
import scrum.client.workspace.VisibleDataChangedEvent;

public class CloseTaskDropAction implements BlockListDropAction<Task> {

	private Requirement requirement;

	public CloseTaskDropAction(Requirement requirement) {
		this.requirement = requirement;
	}

	public boolean onDrop(Task task) {
		Requirement requirement = task.getRequirement();
		task.setRequirement(this.requirement);
		if (!task.isClosed()) task.setDone(Scope.get().getComponent(Auth.class).getUser());
		Scope.get().getComponent(scrum.client.undo.Undo.class).getManager().add(new Undo(task, requirement));
		return true;
	}

	class Undo extends AUndoOperation {

		private Task task;
		private Requirement requirement;

		public Undo(Task task, Requirement requirement) {
			this.task = task;
			this.requirement = requirement;
		}

		@Override
		public String getLabel() {
			return "Undo Close/Change Story for " + task.getReference() + " " + task.getLabel();
		}

		@Override
		protected void onUndo() {
			task.setRequirement(requirement);
			task.setUnDone(Scope.get().getComponent(Auth.class).getUser());
			new VisibleDataChangedEvent().fireInCurrentScope();
		}

	}

}
