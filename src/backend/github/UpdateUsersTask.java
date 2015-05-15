package backend.github;

import backend.interfaces.Repo;
import backend.interfaces.TaskRunner;
import backend.resource.Model;
import backend.resource.TurboUser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import util.Utility;

import java.util.List;

public class UpdateUsersTask extends GitHubRepoTask<GitHubRepoTask.Result> {

	private static final Logger logger = LogManager.getLogger(UpdateUsersTask.class.getName());

	private final Model model;

	public UpdateUsersTask(TaskRunner taskRunner, Repo<Issue, Label, Milestone, User> repo, Model model) {
		super(taskRunner, repo);
		this.model = model;
	}

	@Override
	public void run() {
		ImmutablePair<List<User>, String> changes = repo.getUpdatedUsers(model.getRepoId().generateId(),
			model.getUpdateSignature().labelsETag);

		List<TurboUser> existing = model.getUsers();
		List<User> changed = changes.left;
		logger.info(changed.size() + " user(s) changed" + (changed.size() == 0 ? "" : ": " + changed));

		List<TurboUser> updated = Utility.reconcile(existing, changed,
			TurboUser::getLoginName, User::getLogin, TurboUser::new);

		response.complete(new Result<>(updated, changes.right));
	}
}
