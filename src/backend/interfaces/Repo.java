package backend.interfaces;

import backend.UserCredentials;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.Date;
import java.util.List;

public interface Repo<I, L, M, U> {

	public boolean login(UserCredentials credentials);

	public List<I> getIssues(String repoId);
	public List<L> getLabels(String repoId);
	public List<M> getMilestones(String repoId);
	public List<U> getUsers(String repoId);

	// Returns tuples in order to be maximally generic
	public ImmutableTriple<List<I>, String, Date> getUpdatedIssues(String repoId, String ETag, Date lastCheckTime);
	public ImmutablePair<List<L>, String> getUpdatedLabels(String repoId, String ETag);
	public ImmutablePair<List<M>, String> getUpdatedMilestones(String repoId, String ETag);
	public ImmutablePair<List<U>, String> getUpdatedUsers(String repoId, String ETag);

}
