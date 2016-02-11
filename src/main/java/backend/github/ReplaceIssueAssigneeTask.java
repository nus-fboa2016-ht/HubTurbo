package backend.github;

import backend.interfaces.Repo;
import backend.interfaces.TaskRunner;
import org.eclipse.egit.github.core.Issue;

import java.io.IOException;

public class ReplaceIssueAssigneeTask extends GitHubRepoTask<Issue> {
    private final String repoId;
    private final int issueId;
    private final String issueTitle;
    private final String issueAssigneeLoginName;

    public ReplaceIssueAssigneeTask(TaskRunner taskRunner, Repo repo, String repoId, int issueId,
                                    String issueTitle, String issueAssigneeLoginName) {
        super(taskRunner, repo);
        this.repoId = repoId;
        this.issueId = issueId;
        this.issueTitle = issueTitle;
        this.issueAssigneeLoginName = issueAssigneeLoginName;
    }

    @Override
    public void run() {
        try {
            response.complete(repo.setAssignee(repoId, issueId, issueTitle, issueAssigneeLoginName));
        } catch (IOException e) {
            response.completeExceptionally(e);
        }
    }
}
