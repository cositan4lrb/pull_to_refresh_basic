package com.hust.dapang.pull_to_refresh_basic;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;


public class PullToRefreshBasicLib extends ListView implements AbsListView.OnScrollListener {

	private LinearLayout mHeaderView; // 整个头布局对象
	private View mCustomHeaderView; // 添加的自定义头布局
	private int downY = -1; // 按下时刻y轴的偏移量
	private int mPullDownHeaderViewHeight; // 下拉头布局的高度
	private View mPullDownHeaderView; // 下拉头布局的view对象

	//定义几个变量　用以区分下拉刷新的状态
	private final int PULL_DOWN = 0; // 下拉刷新
	private final int RELEASE_REFRESH = 1; // 释放刷新
	private final int REFRESHING = 2; // 正在刷新中..

	private int currentState = PULL_DOWN; // 当前下拉头布局的状态, 默认为: 下拉刷新状态

	private RotateAnimation upAnim; // 向上旋转的动画
	private RotateAnimation downAnim; // 向下旋转的动画
	private ImageView ivArrow; // 头布局的箭头
	private ProgressBar mProgressbar; // 头布局的进度圈
	private TextView tvState; // 头布局的状态
	private TextView tvLastUpdateTime; // 头布局的最后刷新时间
	private int mListViewYOnScreen = -1; // ListView在屏幕中y轴的值 初始值为1

	private OnRefreshListener mOnRefreshListener; // 下拉刷新和加载更多的回调接口
	private View mFooterView; // 脚布局对象
	private int mFooterViewHeight; // 脚布局的高度
	private boolean isLoadingMore = false; // 是否正在加载更多中, 默认为: false
	private boolean isEnabledPullDownRefresh = false;//是否启用下拉刷新 默认为false
	private boolean isEnabledLoadingMore = false;//是否启用加载更多 默认为false

	public PullToRefreshBasicLib(Context context) {
		super(context);
		initHeader();
		initFooter();
	}

	public PullToRefreshBasicLib(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHeader();
		initFooter();
	}

	public PullToRefreshBasicLib(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initHeader();
		initFooter();
	}

	/**
	 * 初始化脚布局
	 */
	private void initFooter() {
		mFooterView = View.inflate(getContext(), R.layout.refresh_footer_view, null);
		mFooterView.measure(0, 0);
		mFooterViewHeight = mFooterView.getMeasuredHeight();
		mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);

		//addFooterView是ListView中自带的方法
		this.addFooterView(mFooterView);

		// 给当前Listview设置一个滑动的监听事件
		this.setOnScrollListener(this);
	}

	/**
	 * 初始化下拉刷新头布局
	 */
	private void initHeader() {
		mHeaderView = (LinearLayout) View.inflate(getContext(), R.layout.refresh_header_view, null);
		mPullDownHeaderView = mHeaderView.findViewById(R.id.ll_refresh_header_view_pull_down);
		ivArrow = (ImageView) mHeaderView.findViewById(R.id.iv_refresh_header_view_pull_down_arrow);
		mProgressbar = (ProgressBar) mHeaderView.findViewById(R.id.pb_refresh_header_view_pull_down);
		tvState = (TextView) mHeaderView.findViewById(R.id.tv_refresh_header_view_pull_down_state);
		tvLastUpdateTime = (TextView) mHeaderView.findViewById(R.id.tv_refresh_header_view_pull_down_last_update_time);

		//设置一下刚进入时 默认的时间
		tvLastUpdateTime.setText("最后刷新时间:" + getCurrentTime());

		// 测量下拉刷新头的高度.
		mPullDownHeaderView.measure(0, 0);
		// 得到下拉刷新头布局的高度 用测量的办法
		mPullDownHeaderViewHeight = mPullDownHeaderView.getMeasuredHeight();
		System.out.println("头布局的高度: " + mPullDownHeaderViewHeight);

		// 隐藏头布局 注意 是把下拉刷新头布局隐藏掉 不要用总的头部局去操作 否则后面进行计算的时候 会重复计算
		mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);

		this.addHeaderView(mHeaderView);

		// 初始化动画
		initAnimation();
	}

	//下拉刷新动画效果
	//逆时针就是负数
	private void initAnimation() {
		upAnim = new RotateAnimation(
				0, -180,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		upAnim.setDuration(500);
		upAnim.setFillAfter(true);//动画结束后 停在结束的位置上

		downAnim = new RotateAnimation(
				-180, -360,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		downAnim.setDuration(500);
		downAnim.setFillAfter(true);
	}

	/**
	 * 添加一个自定义的头布局.不用传统的addheaderview 为的是把轮播图加进去
	 *
	 * @param v
	 */
	public void addCustomHeaderView(View v) {
		this.mCustomHeaderView = v;
		mHeaderView.addView(v);
	}

	/**
	 * 实现下拉刷新的触摸方法
	 *
	 * @param ev
	 * @return
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				downY = (int) ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				//这里需要严谨一点 因为存在误触的情况 一旦没有取到值（缺省值为-1） 就再取一次
				if (downY == -1) {
					downY = (int) ev.getY();
				}

				//如果没有启用下拉刷新功能 直接跳出switch
				if (!isEnabledPullDownRefresh) {
					break;
				}
				//除了ListView文章列表页面需要屏蔽下拉回弹 当正在刷新的时候 也要屏蔽 否则会导致刷新两次
				if (currentState == REFRESHING) {
					// 当前正在刷新中, 跳出switch
					break;
				}

				// 判断添加的轮播图是否完全显示了, 如果没有完全显示,
				// 不执行下面下拉头的代码, 跳出switch语句, 执行父元素的touch事件.

				//》》难点在于 如何判断现在的轮播图完全显示了 解决办法是 用轮播图的上方Y值去和ListView的上方Y值进行比较《《
				if (mCustomHeaderView != null) {
					int[] location = new int[2]; // 0位是x轴的值, 1位是y轴的值

					if (mListViewYOnScreen == -1) {
						// 获取Listview在屏幕中y轴的值.
						this.getLocationOnScreen(location);
						mListViewYOnScreen = location[1];
//					System.out.println("ListView在屏幕中的y轴的值: " + mListViewYOnScreen);
					}

					// 获取mCustomHeaderView（就是轮播图）在屏幕y轴的值.
					mCustomHeaderView.getLocationOnScreen(location);
					int mCustomHeaderViewYOnScreen = location[1];
//				System.out.println("mCustomHeaderView在屏幕中的y轴的值: " + mCustomHeaderViewYOnScreen);

					if (mListViewYOnScreen > mCustomHeaderViewYOnScreen) {
//					System.out.println("没有完全显示.");
						break; //直接跳到父类方法里 super
					}
				}

				//获得当前手指移动到的点位置
				int moveY = (int) ev.getY();

				// 移动的差值
				int diffY = moveY - downY;

				/**
				 * 双重条件判断触发下拉刷新
				 * 如果diffY差值大于0, 向下拖拽
				 * 并且 当前ListView可见的第一个条目的索引等于0 索引就是ListView中的条目编号
				 * 才进行下拉头的操作
				 */
				if (diffY > 0 && getFirstVisiblePosition() == 0) {
					//》》》核心公式《《《用负的头部局高度+移动的间距 = 一个负值 作为paddingtop
					int paddingTop = -mPullDownHeaderViewHeight + diffY;
					//	System.out.println("paddingTop: " + paddingTop);


					//利用双条件判断 区分出进入“时刻” 和离开“时刻” 让动画只执行一次
					if (paddingTop > 0 && currentState != RELEASE_REFRESH) {
						System.out.println("完全显示了, 进入到释放刷新");
						currentState = RELEASE_REFRESH;
						refreshPullDownHeaderState();
					} else if (paddingTop < 0 && currentState != PULL_DOWN) {
						System.out.println("部分显示了, 进入到下拉刷新");
						currentState = PULL_DOWN;
						refreshPullDownHeaderState();
					}

					mPullDownHeaderView.setPadding(0, paddingTop, 0, 0);
					//这里返回true 否则进入父类方法
					return true;
				}
				break;

			//抬起的时候有两种可能 一种是头布局没有完全暴露时的 回弹 一种是头部局完全暴露以后 显示圆圈的滚动
			case MotionEvent.ACTION_UP:
				downY = -1;

				if (currentState == PULL_DOWN) {
					// 当前状态是下拉刷新状态, 把头布局隐藏.
					mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);
				} else if (currentState == RELEASE_REFRESH) {
					// 当前状态是释放刷新, 把头布局完全显示, 并且进入到正在刷新中状态
					mPullDownHeaderView.setPadding(0, 0, 0, 0);
					currentState = REFRESHING;
					refreshPullDownHeaderState();

					// 调用用户的回调接口
					if (mOnRefreshListener != null) {
						mOnRefreshListener.onPullDownRefresh();
					}
				}
				break;
			default:
				break;
		}
		//注意 这里的 super不能删 因为是继承父类 删了 则不是下拉刷新时的普通ListView滚动会失效
		return super.onTouchEvent(ev);
	}

	/**
	 * 根据currentState当前的状态, 来刷新头布局的状态
	 */
	private void refreshPullDownHeaderState() {
		switch (currentState) {
			case PULL_DOWN: // 下拉刷新状态
				ivArrow.startAnimation(downAnim);
				tvState.setText("下拉刷新");
				break;
			case RELEASE_REFRESH: // 释放刷新状态
				ivArrow.startAnimation(upAnim);
				tvState.setText("释放刷新");
				break;
			case REFRESHING: // 正在刷新中
				ivArrow.clearAnimation();
				ivArrow.setVisibility(View.INVISIBLE);
				mProgressbar.setVisibility(View.VISIBLE);
				tvState.setText("正在刷新中..");
				break;
			default:
				break;
		}
	}

	/**
	 * 当数据刷新完成时调用此方法 用来隐藏头布局
	 */
	public void onRefreshFinish() {

		//用isLoadingMore作为判断变量 对应头布局和脚布局
		if (isLoadingMore) {
			// 当前是加载更多的操作, 隐藏脚布局
			isLoadingMore = false;
			mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);
		} else {
			// 当前是下拉刷新的操作, 隐藏头布局和复位变量.
			mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);
			currentState = PULL_DOWN;
			mProgressbar.setVisibility(View.INVISIBLE);
			ivArrow.setVisibility(View.VISIBLE);
			tvState.setText("下拉刷新");
			tvLastUpdateTime.setText("最后刷新时间: " + getCurrentTime());
		}

	}

	/**
	 * 获取当前时间, 格式为: 1990-09-09 09:09:09
	 *
	 * @return
	 */
	private String getCurrentTime() {
		//Java里面的格式化器 用来提供出格式化的系统时间
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(new Date());
	}

	/**
	 * 设置刷新的监听事件
	 *
	 * @param listener
	 */
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}



	/**
	 * @author dapang
	 *         刷新回调接口
	 */
	public interface OnRefreshListener {

		/**
		 * 当下拉刷新时 触发此方法, 实现此方法是抓取数据.
		 */
		public void onPullDownRefresh();

		/**
		 * 当加载更多时, 触发此方法.
		 */
		public void onLoadingMore();

	}

	/**
	 * 脚布局显示判断
	 * 当滚动的状态改变时触发此方法.
	 * scrollState 当前的状态
	 * SCROLL_STATE_IDLE 停滞
	 * SCROLL_STATE_TOUCH_SCROLL 触摸滚动
	 * SCROLL_STATE_FLING 惯性滑动(猛的一滑)
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {

		if(!isEnabledLoadingMore){
			//当前没有启用加载更多功能
			return;
		}

		if (scrollState == SCROLL_STATE_IDLE
				|| scrollState == SCROLL_STATE_FLING) {
			int lastVisiblePosition = getLastVisiblePosition();

			//注意这里的条件判断！ 避免反复
			if ((lastVisiblePosition == getCount() - 1) && !isLoadingMore) {
				System.out.println("滑动到底部了");

				//检测目前是否是加载更多状态 避免反复请求网络
				isLoadingMore = true;

				mFooterView.setPadding(0, 0, 0, 0);
				// 把脚布局显示出来, 把ListView滑动到最低边
				this.setSelection(getCount());

				if (mOnRefreshListener != null) {
					mOnRefreshListener.onLoadingMore();
				}
			}
		}

	}

	/**
	 * 当滚动时触发此方法
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
						 int visibleItemCount, int totalItemCount) {

	}

	/**
	 * 是否启用下拉刷新
	 *
	 * @param isEnabled
	 */
	public void isEnabledPullDownRefresh(boolean isEnabled) {
		isEnabledPullDownRefresh = isEnabled;
	}


	/**
	 * 是否启用加载更多
	 * @param isEnabled
	 */
	public void isEnabledLoadingMore(boolean isEnabled) {
		isEnabledLoadingMore = isEnabled;
	}


}