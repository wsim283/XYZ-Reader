package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.example.xyzreader.GeneralUtil;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = ArticleListActivity.class.toString();
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private CoordinatorLayout coordinatorLayout;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private int clickedItemPos = -1;
    private boolean isReturn = false;
    private int endPosition = -1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);


        ActivityCompat.setExitSharedElementCallback(this, new MyExitSharedElementCallback());
        getLoaderManager().initLoader(0, null, ArticleListActivity.this);

        if (savedInstanceState == null) {
            refresh();
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.has_connection));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshingReceiver, intentFilter);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mRecyclerView.setVisibility(View.VISIBLE);
                mRecyclerView.setEnabled(true);
                GeneralUtil.debugLog(LOG_TAG, "onReceive called");
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }else if(getString(R.string.has_connection).equals(intent.getAction())){
                //Since the starter code already checks for connectivity in UpdaterService,
                //we can use this same method and handles it by sending a broadcast.
                //upon receive, we can then setup the offline message to our snackbar
                //On top of that, we need to hide our recycler view in cases when the app was launched successfully before
                //loss of connectivity. This is because it'll still load up the items due to androids cache (but without content)
                //which still allows user to click the item which is not what we want
                GeneralUtil.debugLog(LOG_TAG, "onReceive called, no connection detected");
                mRecyclerView.setVisibility(View.INVISIBLE);
                mRecyclerView.setEnabled(false);
                Snackbar snackbar = Snackbar.make(coordinatorLayout, getString(R.string.no_internet_connection_msg), Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(getString(R.string.retry_refresh), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refresh();
                    }
                });
                snackbar.show();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        GeneralUtil.debugLog(LOG_TAG, "onLoadFinished");
        Adapter adapter = new Adapter(cursor, this);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount, GridLayoutManager.VERTICAL,false);
        mRecyclerView.setLayoutManager(gridLayoutManager);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    /**
     * We'll need to set up our transition when we return back to this activity
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            isReturn = true;
            endPosition = data.getIntExtra(getString(R.string.end_position_extra), -1);
            clickedItemPos = data.getIntExtra(getString(R.string.start_position_extra), -1);

            ActivityCompat.postponeEnterTransition(this);
            GeneralUtil.debugLog(LOG_TAG, "re-entering list activity, endPosition" + endPosition);

            //Delays preDraw and scrollToPosition
            //This is because during an orientation change in Detail Activity,
            //the return transition fails to execute if the returning item is off-screen.
            //Through tests/inspection, this is because the loader has not loaded the data in-time for the preDraw & scrollToPosition to occur
            //Through tests/inspection, 100ms delay gives the most optimised result.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(endPosition != 1)
                        mRecyclerView.scrollToPosition(endPosition);
                    mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                            mRecyclerView.requestLayout();
                            ActivityCompat.startPostponedEnterTransition(ArticleListActivity.this);
                            return true;
                        }
                    });

                }
            }, 100);
        }
    }


    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;
        private ArticleListActivity articleListActivity;
        private final String LOG_TAG = Adapter.class.getSimpleName();
        public Adapter(Cursor cursor, ArticleListActivity articleListActivity) {

            mCursor = cursor;
            this.articleListActivity = articleListActivity;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }



        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            GeneralUtil.debugLog(LOG_TAG, "onCreateViewHolder called");
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickedItemPos = vh.getAdapterPosition();
                    long itemId = getItemId(clickedItemPos);
                    Intent detailIntent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(itemId));

                    detailIntent.putExtra(getString(R.string.start_position_extra), clickedItemPos);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                        Bundle bundleForTransition = ActivityOptionsCompat
                                .makeSceneTransitionAnimation(
                                        articleListActivity,
                                        vh.thumbnailView,
                                        vh.thumbnailView.getTransitionName())
                                .toBundle();
                        startActivity(detailIntent,
                                bundleForTransition);

                    }else {
                        startActivity(detailIntent);
                    }
                }
            });

            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(LOG_TAG, ex.getMessage());
                Log.i(LOG_TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            String thumbnailTransitionName = getString(R.string.poster_transition, mRecyclerView.getAdapter().getItemId(position));
            holder.thumbnailView.setTag(thumbnailTransitionName);

            GeneralUtil.debugLog(LOG_TAG, "position: "+ position + "   transitionName: "+thumbnailTransitionName);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                holder.thumbnailView.setTransitionName(thumbnailTransitionName);

        }

        @Override
        public int getItemCount() {
            return (mCursor!=null)?mCursor.getCount():0;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    private class MyExitSharedElementCallback extends SharedElementCallback{

        private final String LOG_TAG = MyExitSharedElementCallback.class.getSimpleName();

        /**After testing, this seems to get called whenever the transition is starting/returning.
         //This also applies for setEnterSharedElementCallback in ArticleDetailActivity
         //After researching the DOC, exitSharedElement is the element for the exit activity, in this case ArticleListActivity
         //enterSharedElement is for the entering activity of the transition(ArticleDetailActivity)
         //We need to modify the shared element because of cases when users swipe left or right.
         //The fragment is no longer the same as the one we entered with and so we need to set the correct view to end the transition appropriately
         **/
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            //As mentioned, this callback gets triggered whenever the transition is starting/returning
            //We only want to modify the element when it is returning AND the position has changed
            if(isReturn) {

                //position determines which item in the list that we need to apply the return transition
                //if user didn't swipe anywhere in the detail activity then keep it at the same clicked item position
                //else, set it to the correct item that the user swiped to
                int position = clickedItemPos;
                if(endPosition != clickedItemPos && endPosition != -1) {
                    position = endPosition;
                }

                String transitionName = getString(R.string.poster_transition, mRecyclerView.getAdapter().getItemId(position));
                GeneralUtil.debugLog(LOG_TAG, "position: "+ position + "   transitionName: "+transitionName);
                View endThumbnailView = mRecyclerView.findViewWithTag(transitionName);

                if (endThumbnailView != null) {
                    names.clear();
                    names.add(transitionName);
                    sharedElements.clear();
                    sharedElements.put(transitionName, endThumbnailView);
                }
                //Set it back, otherwise we might modify it by accident when the user clicks another item
                isReturn = false;
            }
        }
    }
}
