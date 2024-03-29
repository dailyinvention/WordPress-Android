package org.wordpress.android.ui.comments;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;

public class ViewCommentFragment extends Fragment {

    private Drawable d;
    private OnCommentStatusChangeListener onCommentStatusChangeListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            onCommentStatusChangeListener = (OnCommentStatusChangeListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement NoteSelectedCallback");
        }
    }

    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {

            super.handleMessage(msg);

            try {
                final ImageView ivGravatar = (ImageView) getActivity()
                        .findViewById(R.id.gravatar);
                ivGravatar.setImageDrawable(d);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    };

    private void getGravatar(final String gravatarURL) {

        new Thread() {

            public void run() {

                d = getDrawable(gravatarURL);

                handler.sendEmptyMessage(0);

            }

        }.start();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (WordPress.currentComment != null) {
            loadComment(WordPress.currentComment);
            processCommentStatus();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.view_comment, container, false);

        ImageButton delete = (ImageButton) v.findViewById(R.id.deleteComment);

        delete.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                onCommentStatusChangeListener.onCommentStatusChanged("delete");
            }
        });

        Button spam = (Button) v.findViewById(R.id.markSpam);

        spam.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                onCommentStatusChangeListener.onCommentStatusChanged("spam");
            }
        });

        Button unapprove = (Button) v.findViewById(R.id.unapproveComment);

        unapprove.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                onCommentStatusChangeListener.onCommentStatusChanged("hold");
            }
        });

        Button approve = (Button) v.findViewById(R.id.approveComment);

        approve.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                onCommentStatusChangeListener.onCommentStatusChanged("approve");
            }
        });

        Button reply = (Button) v.findViewById(R.id.reply);

        reply.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                onCommentStatusChangeListener.onCommentStatusChanged("reply");
            }
        });

        Button editCommentButton = (Button) v.findViewById(R.id.editComment);
        editCommentButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (WordPress.currentComment != null) {
                    Intent i = new Intent(
                            getActivity().getApplicationContext(),
                            EditCommentActivity.class);
                    startActivityForResult(i, 0);
                }

            }
        });

        return v;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case 0:
            // Our user has returned from editing this comment.
            if (resultCode == 0) {
                // nothing to do?
            }
            break;
        }

    }

    private void processCommentStatus() {

        Button approve = (Button) getActivity().findViewById(R.id.approveComment);
        Button unapprove = (Button) getActivity().findViewById(R.id.unapproveComment);
        Button spam = (Button) getActivity().findViewById(R.id.markSpam);

        // hide buttons based on comment status
        if (WordPress.currentComment != null) {
            if (WordPress.currentComment.status.equals("hold")) {
                unapprove.setVisibility(View.GONE);
                approve.setVisibility(View.VISIBLE);
                spam.setVisibility(View.VISIBLE);
            } else if (WordPress.currentComment.status.equals("approve")) {
                approve.setVisibility(View.GONE);
                unapprove.setVisibility(View.VISIBLE);
                spam.setVisibility(View.VISIBLE);
            } else if (WordPress.currentComment.status.equals("spam")) {
                spam.setVisibility(View.GONE);
                approve.setVisibility(View.VISIBLE);
                unapprove.setVisibility(View.VISIBLE);
            }
        }

    }

    public Drawable getDrawable(String imgUrl) {
        try {
            URL url = new URL(imgUrl);
            InputStream is = null;
            try {
                is = (InputStream) url.getContent();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (is != null) {
                Drawable d = Drawable.createFromStream(is, "src");
                return d;
            } else {
                return null;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getMd5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);

            while (md5.length() < 32)
                md5 = "0" + md5;

            return md5;
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5", e.getMessage());
            return null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    public void loadComment(Comment comment) {

        WordPress.currentComment = comment;

        final String gravatarURL = "http://gravatar.com/avatar/"
                + getMd5Hash(comment.authorEmail) + "?s=200&d=mm";

        getGravatar(gravatarURL);

        getActivity().setTitle(
                getResources().getText(R.string.view_comment_from) + " "
                        + comment.name);

        TextView tvName = (TextView) getActivity().findViewById(
                R.id.commentDetailName);

        String authName = comment.name;
        if (authName.length() <= 0)
            tvName.setText(R.string.anonymous);
        else
            tvName.setText(comment.name);

        TextView tvEmail = (TextView) getActivity().findViewById(
                R.id.commentDetailEmail);
        if (!comment.authorEmail.equals("")) {
            tvEmail.setText(comment.authorEmail);
            Linkify.addLinks(tvEmail, Linkify.ALL);
        } else {
            tvEmail.setVisibility(View.GONE);
        }

        TextView tvURL = (TextView) getActivity().findViewById(
                R.id.commentDetailUrl);
        if (!comment.authorURL.equals("")) {
            tvURL.setText(comment.authorURL);
            Linkify.addLinks(tvURL, Linkify.ALL);
        } else {
            tvURL.setVisibility(View.GONE);
        }

        TextView tvComment = (TextView) getActivity().findViewById(
                R.id.commentDetailComment);

        tvComment.setText(comment.comment);
        Linkify.addLinks(tvComment, Linkify.ALL);

        TextView tvDate = (TextView) getActivity().findViewById(
                R.id.commentDetailDate);

        tvDate.setText(comment.dateCreatedFormatted);

        TextView tvPost = (TextView) getActivity().findViewById(
                R.id.commentDetailPost);

        tvPost.setText(comment.postTitle);
        processCommentStatus();

    }

    public interface OnCommentStatusChangeListener {
        public void onCommentStatusChanged(String status);
    }

    public void setOnCommentStatusChangeListener(
            OnCommentStatusChangeListener listener) {
        onCommentStatusChangeListener = listener;
    }

    public void clearContent() {
        TextView tvName = (TextView) getActivity().findViewById(
                R.id.commentDetailName);

        tvName.setText("");

        TextView tvEmail = (TextView) getActivity().findViewById(
                R.id.commentDetailEmail);
        tvEmail.setText("");

        TextView tvURL = (TextView) getActivity().findViewById(
                R.id.commentDetailUrl);
        tvURL.setText("");

        TextView tvComment = (TextView) getActivity().findViewById(
                R.id.commentDetailComment);
        tvComment.setText("");

        TextView tvDate = (TextView) getActivity().findViewById(
                R.id.commentDetailDate);
        tvDate.setText("");

        TextView tvPost = (TextView) getActivity().findViewById(
                R.id.commentDetailPost);
        tvPost.setText("");

        ImageView ivGravatar = (ImageView) getActivity()
                .findViewById(R.id.gravatar);
        ivGravatar.setImageDrawable(null);


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}
