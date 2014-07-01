package util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;

public class GitHubClientExtended extends GitHubClient{
	public GitHubClientExtended(){
		
	}
	
	public HttpURLConnection createConnection(GitHubRequest request) throws IOException{
		HttpURLConnection connection = createGet(request.generateUri());
		return connection;
	}
	
	
	public HttpURLConnection createPost(String uri) throws IOException {
		return super.createPost(uri);
	}

	
	public InputStream getStream(HttpURLConnection request)
			throws IOException {
		return super.getStream(request);
	}
	
	public Object getBody(GitHubRequest request, InputStream stream)
			throws IOException {
		return super.getBody(request, stream);
	}
	
	protected boolean isError(final int code) {
		return super.isError(code);
	}
	
	protected boolean isOk(final int code) {
		return super.isOk(code);
	}

	
	public void sendParams(HttpURLConnection request, Object params)
			throws IOException {
		super.sendParams(request, params);
	}
}
