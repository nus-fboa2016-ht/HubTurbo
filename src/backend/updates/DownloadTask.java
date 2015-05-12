package backend.updates;

import backend.Model;
import backend.TurboIssue;
import backend.UpdateSignature;
import backend.interfaces.Repo;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryId;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DownloadTask extends RepoTask<Model> {

	private final String repoId;

	public DownloadTask(BlockingQueue<RepoTask<?>> tasks, Repo repo, String repoId) {
		super(tasks, repo);
		this.repoId = repoId;
	}

	@Override
	public void update() {
		List<TurboIssue> issues = repo.getIssues(repoId).stream()
			.map(TurboIssue::new)
			.collect(Collectors.toList());
		Model result = new Model(RepositoryId.createFromId(repoId), issues, UpdateSignature.empty);
		response.complete(result);
	}
}
