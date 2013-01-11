package org.wordpress.android;

import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.StringHelper;
import org.wordpress.android.util.WPHtml;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

public class ViewPostFragment extends Fragment {
	/** Called when the activity is first created. */

	private OnDetailPostActionListener onDetailPostActionListener;
	Posts parentActivity;

	@Override
	public void onActivityCreated(Bundle bundle) {
		super.onActivityCreated(bundle);

	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (WordPress.currentPost != null)
			loadPost(WordPress.currentPost);
		
		parentActivity = (Posts) getActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.viewpost, container, false);
		
		// button listeners here
		ImageButton editPostButton = (ImageButton) v
				.findViewById(R.id.editPost);
		editPostButton.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {
				if (WordPress.currentPost != null && !parentActivity.isRefreshing) {
					onDetailPostActionListener.onDetailPostAction(
							Posts.POST_EDIT, WordPress.currentPost);
					Intent i = new Intent(
							getActivity().getApplicationContext(),
							EditPost.class);
					i.putExtra("isPage", WordPress.currentPost.isPage());
					i.putExtra("postID", WordPress.currentPost.getId());
					i.putExtra("localDraft", WordPress.currentPost.isLocalDraft());
					startActivityForResult(i, 0);
				}

			}
		});
		
		ImageButton shareURLButton = (ImageButton) v
				.findViewById(R.id.sharePostLink);
		shareURLButton.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {

				if (!parentActivity.isRefreshing)
					onDetailPostActionListener.onDetailPostAction(Posts.POST_SHARE, WordPress.currentPost);

			}
		});

		ImageButton deletePostButton = (ImageButton) v
				.findViewById(R.id.deletePost);
		deletePostButton.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {
				
				if (!parentActivity.isRefreshing)
					onDetailPostActionListener.onDetailPostAction(Posts.POST_DELETE, WordPress.currentPost);

			}
		});

		ImageButton viewPostButton = (ImageButton) v
				.findViewById(R.id.viewPost);
		viewPostButton.setOnClickListener(new ImageButton.OnClickListener() {
			public void onClick(View v) {
				
				if (!parentActivity.isRefreshing)
					loadPostPreview();

			}
		});

		return v;

	}

	protected void loadPostPreview() {

		if (WordPress.currentPost != null) {
			if (!WordPress.currentPost.getPermaLink().equals("")) {
				Intent i = new Intent(getActivity(), Read.class);
				startActivity(i);
			}
		}

	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// check that the containing activity implements our callback
			onDetailPostActionListener = (OnDetailPostActionListener) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement Callback");
		}
	}

	public void loadPost(Post post) {

		// Don't load if the Post object of title are null, see #395
		if (post == null || post.getTitle() == null)
			return;
		
		TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
		if (post.getTitle().equals(""))
			title.setText("(" + getResources().getText(R.string.untitled) + ")");
		else
			title.setText(EscapeUtils.unescapeHtml(post.getTitle()));

		WebView webView = (WebView) getActivity().findViewById(
				R.id.viewPostWebView);
		TextView tv = (TextView) getActivity().findViewById(
				R.id.viewPostTextView);
		ImageButton shareURLButton = (ImageButton) getActivity().findViewById(
				R.id.sharePostLink);
		ImageButton viewPostButton = (ImageButton) getActivity().findViewById(
				R.id.viewPost);

		if (post.isLocalDraft()) {
			tv.setVisibility(View.VISIBLE);
			webView.setVisibility(View.GONE);
			tv.setText(WPHtml.fromHtml(
					(post.getDescription() + post.getMt_text_more()).replaceAll("\uFFFC", ""),
					getActivity().getApplicationContext(), post));
			shareURLButton.setVisibility(View.GONE);
			viewPostButton.setVisibility(View.GONE);
		} else {
			tv.setVisibility(View.GONE);
			webView.setVisibility(View.VISIBLE);
			String html = StringHelper.addPTags(post.getDescription()
					+ "\n\n" + post.getMt_text_more());

			String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\">"
					+ html + "</div></body></html>";
			webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
					"text/html", "utf-8", null);
			shareURLButton.setVisibility(View.VISIBLE);
			viewPostButton.setVisibility(View.VISIBLE);
		}

	}

	public interface OnDetailPostActionListener {
		public void onDetailPostAction(int action, Post post);
	}

	public void clearContent() {
		TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
		title.setText("");
		WebView webView = (WebView) getActivity().findViewById(
				R.id.viewPostWebView);
		TextView tv = (TextView) getActivity().findViewById(
				R.id.viewPostTextView);
		tv.setText("");
		String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\"></div></body></html>";
		webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
				"text/html", "utf-8", null);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (outState.isEmpty()) {
			outState.putBoolean("bug_19917_fix", true);
		}
		super.onSaveInstanceState(outState);
	}

}
