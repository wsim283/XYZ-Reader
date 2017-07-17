package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;
    private int currentPos = -1;
    private int startPos = -1;
    private boolean isReturn = false;
    private  ArticleDetailFragment currentlyShownFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }


        ActivityCompat.postponeEnterTransition(this);


        //Since we only want to manipulate the way the transition returns (because returning transition is screwed up if user swipes to diff fragment),
        //we'll check for boolean isReturn,
        //which is set to true at the end in finishAfterTransition()
        //According to the DOC, this callback is set to the the Enter Activity, aka Launched Activity.
        //For the Launching Activity, we need to use setExitSharedElementCallback instead
        ActivityCompat.setEnterSharedElementCallback(this, new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {

              if(isReturn && currentlyShownFragment != null){
                  Log.v("ArticleDetailActivity", "transition triggered");
                  //if the user swipes the pager, we are no longer on the same starting fragment
                  //this means we are not on the same fragment positioned from the start, so we will need to fix up our shared element values
                 if(startPos != currentPos){
                     mCursor.moveToPosition(currentPos);
                     Log.v("ArticleDetailActivity", " startPos: " + startPos);
                    long itemId = mCursor.getLong(ArticleLoader.Query._ID);
                     String transitionName = getString(R.string.poster_transition,itemId);
                     String startTransitionName = getString(R.string.poster_transition,mStartId);
                     names.clear();
                     names.add(transitionName);
                     sharedElements.remove(startTransitionName);
                     sharedElements.put(transitionName, currentlyShownFragment.getView().findViewById(R.id.photo));
                 }

                 isReturn = false;
              }

            }
        });
        setContentView(R.layout.activity_article_detail);


        currentPos = getIntent().getIntExtra(getString(R.string.start_position_extra),-1);
        startPos = currentPos;

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);
            }

            @Override
            public void onPageSelected(int position) {
                currentPos = position;
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                //updateUpButtonPosition();
            }
        });

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               finishAfterTransition();
            }
        });

        /**
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }
**/
        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }else{
            if(savedInstanceState.containsKey(getString(R.string.article_detail_end_position))){
                currentPos = savedInstanceState.getInt(getString(R.string.article_detail_end_position));
                Log.v("ArticleDetailActivity","orientation changed, retrieved saved current position: " + currentPos);
            }

            if(savedInstanceState.containsKey(getString(R.string.article_detail_start_position))){
                startPos = savedInstanceState.getInt(getString(R.string.article_detail_start_position));
                Log.v("ArticleDetailActivity","orientation changed, retrieved saved start position: " + startPos);
            }
        }
    }

    @Override
    public void finishAfterTransition() {
        isReturn = true;
        Log.v("ArticleDetailActivity","finish after transition");
        setResult(RESULT_OK, new Intent().putExtra(getString(R.string.end_position_extra), currentPos)
        .putExtra(getString(R.string.start_position_extra), startPos));
        super.finishAfterTransition();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(getString(R.string.article_detail_end_position), currentPos);
        outState.putInt(getString(R.string.article_detail_start_position), startPos);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();
        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;

        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            //updateUpButtonPosition();
        }
    }

    /**
    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }
     **/

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        FragmentManager fm;
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            this.fm = fm;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                //updateUpButtonPosition();
                //For transition, we need reference to the primary(current) fragment,
                //so that we can access the ImageView that interacts with it.
                currentlyShownFragment = fragment;
               Log.v("TEST", "setPrimaryItem called");
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));

        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
