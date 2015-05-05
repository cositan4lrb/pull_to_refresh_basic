package com.hust.dapang.pull_to_refresh_basic;

import android.content.Context;
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

	private LinearLayout mHeaderView; // ����ͷ���ֶ���
	private View mCustomHeaderView; // ��ӵ��Զ���ͷ����
	private int downY = -1; // ����ʱ��y���ƫ����
	private int mPullDownHeaderViewHeight; // ����ͷ���ֵĸ߶�
	private View mPullDownHeaderView; // ����ͷ���ֵ�view����

	//���弸��������������������ˢ�µ�״̬
	private final int PULL_DOWN = 0; // ����ˢ��
	private final int RELEASE_REFRESH = 1; // �ͷ�ˢ��
	private final int REFRESHING = 2; // ����ˢ����..

	private int currentState = PULL_DOWN; // ��ǰ����ͷ���ֵ�״̬, Ĭ��Ϊ: ����ˢ��״̬

	private RotateAnimation upAnim; // ������ת�Ķ���
	private RotateAnimation downAnim; // ������ת�Ķ���
	private ImageView ivArrow; // ͷ���ֵļ�ͷ
	private ProgressBar mProgressbar; // ͷ���ֵĽ���Ȧ
	private TextView tvState; // ͷ���ֵ�״̬
	private TextView tvLastUpdateTime; // ͷ���ֵ����ˢ��ʱ��
	private int mListViewYOnScreen = -1; // ListView����Ļ��y���ֵ ��ʼֵΪ1

	private OnRefreshListener mOnRefreshListener; // ����ˢ�ºͼ��ظ���Ļص��ӿ�
	private View mFooterView; // �Ų��ֶ���
	private int mFooterViewHeight; // �Ų��ֵĸ߶�
	private boolean isLoadingMore = false; // �Ƿ����ڼ��ظ�����, Ĭ��Ϊ: false
	private boolean isEnabledPullDownRefresh = false;//�Ƿ���������ˢ�� Ĭ��Ϊfalse
	private boolean isEnabledLoadingMore = false;//�Ƿ����ü��ظ��� Ĭ��Ϊfalse

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
	 * ��ʼ������ˢ��ͷ����
	 */
	private void initHeader() {
		mHeaderView = (LinearLayout) View.inflate(getContext(), R.layout.refresh_header_view, null);
		mPullDownHeaderView = mHeaderView.findViewById(R.id.ll_refresh_header_view_pull_down);
		ivArrow = (ImageView) mHeaderView.findViewById(R.id.iv_refresh_header_view_pull_down_arrow);
		mProgressbar = (ProgressBar) mHeaderView.findViewById(R.id.pb_refresh_header_view_pull_down);
		tvState = (TextView) mHeaderView.findViewById(R.id.tv_refresh_header_view_pull_down_state);
		tvLastUpdateTime = (TextView) mHeaderView.findViewById(R.id.tv_refresh_header_view_pull_down_last_update_time);

		//����һ�¸ս���ʱ Ĭ�ϵ�ʱ��
		tvLastUpdateTime.setText("���ˢ��ʱ��:" + getCurrentTime());

		// ��������ˢ��ͷ�ĸ߶�.
		mPullDownHeaderView.measure(0, 0);
		// �õ�����ˢ��ͷ���ֵĸ߶� �ò����İ취
		mPullDownHeaderViewHeight = mPullDownHeaderView.getMeasuredHeight();
		System.out.println("ͷ���ֵĸ߶�: " + mPullDownHeaderViewHeight);

		// ����ͷ���� ע�� �ǰ�����ˢ��ͷ�������ص� ��Ҫ���ܵ�ͷ����ȥ���� ���������м����ʱ�� ���ظ�����
		mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);

		this.addHeaderView(mHeaderView);

		// ��ʼ������
		initAnimation();
	}

	/**
	 * ��ʼ���Ų���
	 */
	private void initFooter() {
		mFooterView = View.inflate(getContext(), R.layout.refresh_footer_view, null);
		mFooterView.measure(0, 0);
		mFooterViewHeight = mFooterView.getMeasuredHeight();
		mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);

		//addFooterView��ListView���Դ��ķ���
		this.addFooterView(mFooterView);

		// ����ǰListview����һ�������ļ����¼�
		this.setOnScrollListener(this);
	}


	//����ˢ�¶���Ч��
	//��ʱ����Ǹ���
	private void initAnimation() {
		upAnim = new RotateAnimation(
				0, -180,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		upAnim.setDuration(500);
		upAnim.setFillAfter(true);//���������� ͣ�ڽ�����λ����

		downAnim = new RotateAnimation(
				-180, -360,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		downAnim.setDuration(500);
		downAnim.setFillAfter(true);
	}

	/**
	 * ���һ���Զ����ͷ����.���ô�ͳ��addheaderview Ϊ���ǰ��ֲ�ͼ�ӽ�ȥ
	 */
	public void addCustomHeaderView(View v) {
		this.mCustomHeaderView = v;
		mHeaderView.addView(v);
	}

	/**
	 * ʵ������ˢ�µĴ�������
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				downY = (int) ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				//������Ҫ�Ͻ�һ�� ��Ϊ�����󴥵���� һ��û��ȡ��ֵ��ȱʡֵΪ-1�� ����ȡһ��
				if (downY == -1) {
					downY = (int) ev.getY();
				}

				//���û����������ˢ�¹��� ֱ������switch
				if (!isEnabledPullDownRefresh) {
					break;
				}
				//����ListView�����б�ҳ����Ҫ���������ص� ������ˢ�µ�ʱ�� ҲҪ���� ����ᵼ��ˢ������
				if (currentState == REFRESHING) {
					// ��ǰ����ˢ����, ����switch
					break;
				}

				// �ж���ӵ��ֲ�ͼ�Ƿ���ȫ��ʾ��, ���û����ȫ��ʾ,
				// ��ִ����������ͷ�Ĵ���, ����switch���, ִ�и�Ԫ�ص�touch�¼�.

				//�����ѵ����� ����ж����ڵ��ֲ�ͼ��ȫ��ʾ�� ����취�� ���ֲ�ͼ���Ϸ�Yֵȥ��ListView���Ϸ�Yֵ���бȽϡ���
				if (mCustomHeaderView != null) {
					int[] location = new int[2]; // 0λ��x���ֵ, 1λ��y���ֵ

					if (mListViewYOnScreen == -1) {
						// ��ȡListview����Ļ��y���ֵ.
						this.getLocationOnScreen(location);
						mListViewYOnScreen = location[1];
//					System.out.println("ListView����Ļ�е�y���ֵ: " + mListViewYOnScreen);
					}

					// ��ȡmCustomHeaderView�������ֲ�ͼ������Ļy���ֵ.
					mCustomHeaderView.getLocationOnScreen(location);
					int mCustomHeaderViewYOnScreen = location[1];
//				System.out.println("mCustomHeaderView����Ļ�е�y���ֵ: " + mCustomHeaderViewYOnScreen);

					if (mListViewYOnScreen > mCustomHeaderViewYOnScreen) {
//					System.out.println("û����ȫ��ʾ.");
						break; //ֱ���������෽���� super
					}
				}

				//��õ�ǰ��ָ�ƶ����ĵ�λ��
				int moveY = (int) ev.getY();

				// �ƶ��Ĳ�ֵ
				int diffY = moveY - downY;

				/**
				 * ˫�������жϴ�������ˢ��
				 * ���diffY��ֵ����0, ������ק
				 * ���� ��ǰListView�ɼ��ĵ�һ����Ŀ����������0 ��������ListView�е���Ŀ���
				 * �Ž�������ͷ�Ĳ���
				 */
				if (diffY > 0 && getFirstVisiblePosition() == 0) {
					//���������Ĺ�ʽ�������ø���ͷ���ָ߶�+�ƶ��ļ�� = һ����ֵ ��Ϊpaddingtop
					int paddingTop = -mPullDownHeaderViewHeight + diffY;
					//	System.out.println("paddingTop: " + paddingTop);


					//����˫�����ж� ���ֳ����롰ʱ�̡� ���뿪��ʱ�̡� �ö���ִֻ��һ��
					if (paddingTop > 0 && currentState != RELEASE_REFRESH) {
						System.out.println("��ȫ��ʾ��, ���뵽�ͷ�ˢ��");
						currentState = RELEASE_REFRESH;
						refreshPullDownHeaderState();
					} else if (paddingTop < 0 && currentState != PULL_DOWN) {
						System.out.println("������ʾ��, ���뵽����ˢ��");
						currentState = PULL_DOWN;
						refreshPullDownHeaderState();
					}

					mPullDownHeaderView.setPadding(0, paddingTop, 0, 0);
					//���ﷵ��true ������븸�෽��
					return true;
				}
				break;

			//̧���ʱ�������ֿ��� һ����ͷ����û����ȫ��¶ʱ�� �ص� һ����ͷ������ȫ��¶�Ժ� ��ʾԲȦ�Ĺ���
			case MotionEvent.ACTION_UP:
				downY = -1;

				if (currentState == PULL_DOWN) {
					// ��ǰ״̬������ˢ��״̬, ��ͷ��������.
					mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);
				} else if (currentState == RELEASE_REFRESH) {
					// ��ǰ״̬���ͷ�ˢ��, ��ͷ������ȫ��ʾ, ���ҽ��뵽����ˢ����״̬
					mPullDownHeaderView.setPadding(0, 0, 0, 0);
					currentState = REFRESHING;
					refreshPullDownHeaderState();

					// �����û��Ļص��ӿ�
					if (mOnRefreshListener != null) {
						mOnRefreshListener.onPullDownRefresh();
					}
				}
				break;
			default:
				break;
		}
		//ע�� ����� super����ɾ ��Ϊ�Ǽ̳и��� ɾ�� ��������ˢ��ʱ����ͨListView������ʧЧ
		return super.onTouchEvent(ev);
	}

	/**
	 * ����currentState��ǰ��״̬, ��ˢ��ͷ���ֵ�״̬
	 */
	private void refreshPullDownHeaderState() {
		switch (currentState) {
			case PULL_DOWN: // ����ˢ��״̬
				ivArrow.startAnimation(downAnim);
				tvState.setText("����ˢ��");
				break;
			case RELEASE_REFRESH: // �ͷ�ˢ��״̬
				ivArrow.startAnimation(upAnim);
				tvState.setText("�ͷ�ˢ��");
				break;
			case REFRESHING: // ����ˢ����
				ivArrow.clearAnimation();
				ivArrow.setVisibility(View.INVISIBLE);
				mProgressbar.setVisibility(View.VISIBLE);
				tvState.setText("����ˢ����..");
				break;
			default:
				break;
		}
	}

	/**
	 * ������ˢ�����ʱ���ô˷��� ��������ͷ����
	 */
	public void onRefreshFinish() {

		//��isLoadingMore��Ϊ�жϱ��� ��Ӧͷ���ֺͽŲ���
		if (isLoadingMore) {
			// ��ǰ�Ǽ��ظ���Ĳ���, ���ؽŲ���
			isLoadingMore = false;
			mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);
		} else {
			// ��ǰ������ˢ�µĲ���, ����ͷ���ֺ͸�λ����.
			mPullDownHeaderView.setPadding(0, -mPullDownHeaderViewHeight, 0, 0);
			currentState = PULL_DOWN;
			mProgressbar.setVisibility(View.INVISIBLE);
			ivArrow.setVisibility(View.VISIBLE);
			tvState.setText("����ˢ��");
			tvLastUpdateTime.setText("���ˢ��ʱ��: " + getCurrentTime());
		}

	}

	/**
	 * ��ȡ��ǰʱ��, ��ʽΪ: 1990-09-09 09:09:09
	 * @return
	 */
	private String getCurrentTime() {
		//Java����ĸ�ʽ���� �����ṩ����ʽ����ϵͳʱ��
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(new Date());
	}

	/**
	 * ����ˢ�µļ����¼�
	 */
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}



	/**
	 * @author dapang
	 *         ˢ�»ص��ӿ�
	 */
	public interface OnRefreshListener {

		/**
		 * ������ˢ��ʱ �����˷���, ʵ�ִ˷�����ץȡ����.
		 */
		public void onPullDownRefresh();

		/**
		 * �����ظ���ʱ, �����˷���.
		 */
		public void onLoadingMore();

	}

	/**
	 * �Ų�����ʾ�ж�
	 * ��������״̬�ı�ʱ�����˷���.
	 * scrollState ��ǰ��״̬
	 * SCROLL_STATE_IDLE ͣ��
	 * SCROLL_STATE_TOUCH_SCROLL ��������
	 * SCROLL_STATE_FLING ���Ի���(�͵�һ��)
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {

		if(!isEnabledLoadingMore){
			//��ǰû�����ü��ظ��๦��
			return;
		}

		if (scrollState == SCROLL_STATE_IDLE
				|| scrollState == SCROLL_STATE_FLING) {
			int lastVisiblePosition = getLastVisiblePosition();

			//ע������������жϣ� ���ⷴ��
			if ((lastVisiblePosition == getCount() - 1) && !isLoadingMore) {
				System.out.println("�������ײ���");

				//���Ŀǰ�Ƿ��Ǽ��ظ���״̬ ���ⷴ����������
				isLoadingMore = true;

				mFooterView.setPadding(0, 0, 0, 0);
				// �ѽŲ�����ʾ����, ��ListView��������ͱ�
				this.setSelection(getCount());

				if (mOnRefreshListener != null) {
					mOnRefreshListener.onLoadingMore();
				}
			}
		}

	}

	/**
	 * ������ʱ�����˷���
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
						 int visibleItemCount, int totalItemCount) {
	}

	/**
	 * �Ƿ���������ˢ��
	 */
	public void isEnabledPullDownRefresh(boolean isEnabled) {
		isEnabledPullDownRefresh = isEnabled;
	}


	/**
	 * �Ƿ����ü��ظ���
	 */
	public void isEnabledLoadingMore(boolean isEnabled) {
		isEnabledLoadingMore = isEnabled;
	}

}