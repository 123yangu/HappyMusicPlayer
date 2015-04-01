package com.happyplayer.service;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.happyplayer.async.AsyncTaskHandler;
import com.happyplayer.common.Constants;
import com.happyplayer.model.SongInfo;
import com.happyplayer.model.SongMessage;
import com.happyplayer.observable.ObserverManage;
import com.happyplayer.player.MediaManage;
import com.happyplayer.ui.MainActivity;
import com.happyplayer.ui.R;
import com.happyplayer.util.DataUtil;
import com.happyplayer.util.ImageUtil;
import com.happyplayer.util.MediaUtils;
import com.happyplayer.widget.StopImageView;

//import android.util.Log;

/**
 * 
 * 桌面后台窗口
 * 
 */
public class EasytouchService extends Service implements Observer {
	private Context context;
	private WindowManager wm = null;
	private WindowManager.LayoutParams iconParams = null;

	private WindowManager.LayoutParams mainParams = null;

	// 状态栏高度
	private double stateHeight;
	private float startX = 0, startY = 0;
	private float startRawX = 0, startRawY = 0;
	private int iconViewX = 0, iconViewY = 0;
	private int width, height;

	private View iconView;
	private boolean iconViewShow = false;
	/**
	 * 歌手图片和专辑图片
	 */
	private ImageView singerPicImageView;

	private TextView timeTextView;

	private StopImageView playingStatus;

	private View mainView;
	private boolean mainViewShow = false;
	private ImageButton wmItemExit;
	private ImageButton wmItemPause;
	private ImageButton wmItemPlay;
	private ImageButton wmItemPrev;
	private ImageButton wmItemNext;
	private ImageButton wmItemHome;

	/**
	 * 显示面板倒计时
	 */
	public int EndTime = -1;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		init();
		handler.post(myRunnable);

		ObserverManage.getObserver().addObserver(this);
	}

	private void init() {
		context = EasytouchService.this.getBaseContext();

		// 获取WindowManager
		wm = (WindowManager) getApplicationContext().getSystemService("window");

		width = wm.getDefaultDisplay().getWidth();
		height = wm.getDefaultDisplay().getHeight();

		stateHeight = Math
				.ceil(25 * context.getResources().getDisplayMetrics().density);

		// 设置LayoutParams(全局变量）相关参数
		iconParams = new WindowManager.LayoutParams();
		iconParams.type = 2002;
		iconParams.format = 1;
		iconParams.flags |= 8;
		iconParams.gravity = Gravity.LEFT | Gravity.TOP;// 调整悬浮窗口至左上角

		// 以屏幕左上角为原点，设置x、y初始值
		iconParams.x = Constants.ICON_VIEWX;
		iconParams.y = Constants.ICON_VIEWY;

		iconView = LayoutInflater.from(context).inflate(R.layout.magic_main,
				null, false);

		singerPicImageView = (ImageView) iconView.findViewById(R.id.singer_pic);

		timeTextView = (TextView) iconView.findViewById(R.id.time);

		playingStatus = (StopImageView) iconView
				.findViewById(R.id.playing_status);

		// 设置悬浮窗口长宽数据
		iconParams.width = 140;
		iconParams.height = 140;

		iconParams.alpha = 0.8f;
		iconView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		iconView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 获取相对屏幕的坐标，即以屏幕左上角为原点
				float rawX = event.getRawX();
				// 为状态栏高度 Math.ceil(25
				// * context.getResources().getDisplayMetrics().density))
				float rawY = (float) (event.getRawY() - stateHeight);
				int sumX = (int) (rawX - startRawX);
				int sumY = (int) (event.getRawY() - startRawY);
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// 获取相对View的坐标，即以此View左上角为原点
					startX = event.getX();
					startY = event.getY();
					startRawX = event.getRawX();
					startRawY = event.getRawY();
					iconParams.alpha = 1f;
					wm.updateViewLayout(iconView, iconParams);
					break;
				case MotionEvent.ACTION_UP:
					// Log.i("Log", "sumX=" + sumX + ";sumY=" + sumY);
					iconParams.alpha = 0.6f;
					wm.updateViewLayout(iconView, iconParams);
					if (sumX > -10 && sumX < 10 && sumY > -10 && sumY < 10) {
						addMainView();
					} else {
						float endRawX = rawX - startX;
						float endRawY = rawY - startY;
						if (endRawX < width / 2) {
							if (endRawX > endRawY) {
								updateIconViewPosition(endRawX, 0);
							} else if (endRawX > height - event.getRawY() - 140) {
								updateIconViewPosition(endRawX, (float) (height
										- stateHeight - 140));
							} else {
								updateIconViewPosition(0, endRawY);
							}
						} else {
							if (width - endRawX - 140 > endRawY) {
								updateIconViewPosition(endRawX, 0);
							} else if (width - endRawX - 140 > height
									- event.getRawY() - 140) {
								updateIconViewPosition(endRawX, (float) (height
										- stateHeight - 140));
							} else {
								updateIconViewPosition(width - 140, endRawY);
							}
						}
					}
					startX = 0;
					startY = 0;
					startRawX = 0;
					startRawY = 0;
					break;
				case MotionEvent.ACTION_MOVE:
					if (sumX < -10 || sumX > 10 || sumY < -10 || sumY > 10) {
						updateIconViewPosition(rawX - startX, rawY - startY);
					}
					break;
				default:
					break;
				}
				return true;
			}
		});

		initMainView();
	}

	public void initMainView() {

		// 设置LayoutParams(全局变量）相关参数
		mainParams = new WindowManager.LayoutParams();
		mainParams.type = 2002;
		mainParams.format = 1;
		mainParams.flags |= 8;

		mainParams.alpha = 1f;
		mainParams.x = 0;
		mainParams.y = 0;
		mainParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		mainParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		mainParams.gravity = mainParams.gravity = Gravity.LEFT | Gravity.TOP;

		mainView = LayoutInflater.from(context).inflate(R.layout.magic_menu,
				null);

		wmItemExit = (ImageButton) mainView.findViewById(R.id.wm_item_exit);
		wmItemPause = (ImageButton) mainView.findViewById(R.id.wm_item_pause);
		wmItemPlay = (ImageButton) mainView.findViewById(R.id.wm_item_play);
		wmItemPrev = (ImageButton) mainView.findViewById(R.id.wm_item_prev);
		wmItemNext = (ImageButton) mainView.findViewById(R.id.wm_item_next);

		wmItemHome = (ImageButton) mainView.findViewById(R.id.wm_item_home);
		View bgView = mainView.findViewById(R.id.wm_main_bgview);

		final RelativeLayout wmMainLayout = (RelativeLayout) mainView
				.findViewById(R.id.wm_main_layout);
		wmItemExit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				SongMessage songMessage = new SongMessage();
				songMessage.setType(SongMessage.EXIT);
				ObserverManage.getObserver().setMessage(songMessage);
			}
		});
		wmItemPause.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EndTime = 3000;
				SongMessage songMessage = new SongMessage();
				songMessage.setType(SongMessage.PLAYORSTOPMUSIC);
				ObserverManage.getObserver().setMessage(songMessage);
			}
		});
		wmItemPlay.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EndTime = 3000;
				SongMessage songMessage = new SongMessage();
				songMessage.setType(SongMessage.PLAYORSTOPMUSIC);
				ObserverManage.getObserver().setMessage(songMessage);
			}
		});
		wmItemPrev.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EndTime = 3000;
				SongMessage songMessage = new SongMessage();
				songMessage.setType(SongMessage.PREVMUSIC);
				ObserverManage.getObserver().setMessage(songMessage);
			}
		});
		wmItemNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EndTime = 3000;
				SongMessage songMessage = new SongMessage();
				songMessage.setType(SongMessage.NEXTMUSIC);
				ObserverManage.getObserver().setMessage(songMessage);
			}
		});
		wmItemHome.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				new AsyncTaskHandler() {

					@Override
					protected void onPostExecute(Object result) {
						addIconView();
					}

					@Override
					protected Object doInBackground() throws Exception {
						Intent intent = new Intent(Intent.ACTION_MAIN);
						intent.addCategory(Intent.CATEGORY_LAUNCHER);
						intent.setClass(getBaseContext(), MainActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
								| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

						getApplication().startActivity(intent);
						return null;
					}
				}.execute();

			}
		});

		bgView.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {

				int x = (int) event.getX();
				int y = (int) event.getY();
				if (event.getAction() == MotionEvent.ACTION_UP) {

					if (wmMainLayout.getLeft() < x
							&& x < wmMainLayout.getRight()
							&& wmMainLayout.getTop() < y
							&& y < wmMainLayout.getBottom()) {
					} else {
						addIconView();
					}

				}
				return true;
			}
		});
	}

	protected void addMainView() {
		if (iconView != null && iconView.getParent() != null) {
			wm.removeView(iconView);
		}
		if (mainView != null && mainView.getParent() == null) {
			mainViewShow = true;
			wm.addView(mainView, mainParams);
			loadMainViewData();

			if (EndTime < 0) {
				EndTime = 3000;
				handler.post(upDateVol);
			} else {
				EndTime = 3000;
			}
		}
	}

	Runnable upDateVol = new Runnable() {

		@Override
		public void run() {
			if (EndTime >= 0) {
				EndTime -= 200;
				handler.postDelayed(upDateVol, 200);
			} else {
				if(mainViewShow){
					addIconView();
				}
			}

		}
	};

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			int status = MediaManage.getMediaManage(context).getPlayStatus();
			final SongInfo songInfo = (SongInfo) msg.obj;

			switch (msg.what) {
			case 0:
				// 本地歌曲
				if (songInfo.getType() == SongInfo.LOCAL) {
					new AsyncTaskHandler() {

						@Override
						protected void onPostExecute(Object result) {
							Bitmap bm = (Bitmap) result;
							if (bm != null) {
								singerPicImageView
										.setBackgroundDrawable(new BitmapDrawable(
												bm));
								// 显示专辑封面图片
							} else {
								bm = MediaUtils.getDefaultArtwork(context,
										false);
								singerPicImageView
										.setBackgroundDrawable(new BitmapDrawable(
												bm));// 显示专辑封面图片
							}
						}

						@Override
						protected Object doInBackground() throws Exception {
							return ImageUtil.getFirstArtwork(
									songInfo.getPath(), songInfo.getSid());
						}
					}.execute();

				} else {
					// 网上下载歌曲
				}
				timeTextView.setVisibility(View.INVISIBLE);
				if (status == MediaManage.STOP)
					playingStatus.setVisibility(View.VISIBLE);
				break;
			// 主菜单
			case 1:
				if (status == MediaManage.STOP) {
					if (wmItemPause.getVisibility() != View.INVISIBLE) {
						wmItemPause.setVisibility(View.INVISIBLE);
					}
					if (wmItemPlay.getVisibility() != View.VISIBLE) {
						wmItemPlay.setVisibility(View.VISIBLE);
					}
				} else {
					if (wmItemPause.getVisibility() != View.VISIBLE) {
						wmItemPause.setVisibility(View.VISIBLE);
					}
					if (wmItemPlay.getVisibility() != View.INVISIBLE) {
						wmItemPlay.setVisibility(View.INVISIBLE);
					}
				}

				break;
			}
		}
	};

	private void loadMainViewData() {
		new AsyncTaskHandler() {

			@Override
			protected void onPostExecute(Object result) {

			}

			@Override
			protected Object doInBackground() throws Exception {
				SongInfo songInfo = MediaManage.getMediaManage(context)
						.getPlaySongInfo();
				if (songInfo != null) {
					Message msg = new Message();
					msg.what = 1;
					msg.obj = songInfo;
					mHandler.sendMessage(msg);
				} else {
					if (wmItemPause.getVisibility() != View.INVISIBLE) {
						wmItemPause.setVisibility(View.INVISIBLE);
					}
					if (wmItemPlay.getVisibility() != View.VISIBLE) {
						wmItemPlay.setVisibility(View.VISIBLE);
					}
				}
				return null;
			}
		}.execute();
	}

	protected void addIconView() {
		if (mainView != null && mainView.getParent() != null) {
			wm.removeView(mainView);
		}
		if (iconView != null && iconView.getParent() == null) {
			mainViewShow = false;
			wm.addView(iconView, iconParams);
			loadIconViewData();
		}
	}

	private void loadIconViewData() {
		new AsyncTaskHandler() {

			@Override
			protected void onPostExecute(Object result) {

			}

			@Override
			protected Object doInBackground() throws Exception {
				SongInfo songInfo = MediaManage.getMediaManage(context)
						.getPlaySongInfo();
				if (songInfo != null) {
					Message msg = new Message();
					msg.what = 0;
					msg.obj = songInfo;
					mHandler.sendMessage(msg);
				}
				return null;
			}
		}.execute();
	}

	private void updateIconViewPosition(float x, float y) {
		iconViewX = (int) x;
		iconViewY = (int) y;
		iconParams.x = (int) x;
		iconParams.y = (int) y;
		wm.updateViewLayout(iconView, iconParams);

		new AsyncTaskHandler() {

			@Override
			protected void onPostExecute(Object result) {

			}

			protected Object doInBackground() throws Exception {
				Constants.ICON_VIEWX = iconViewX;
				Constants.ICON_VIEWY = iconViewY;

				DataUtil.save(context, Constants.ICON_VIEWX_KEY,
						Constants.ICON_VIEWX);
				DataUtil.save(context, Constants.ICON_VIEWY_KEY,
						Constants.ICON_VIEWY);
				return null;
			}
		}.execute();
	}

	private Handler floatViewHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (!Constants.SHOWEASYTOUCH)
				return;
			switch (msg.what) {
			// 程序后台运行
			case 0:
				if (!iconViewShow && iconView.getParent() == null) {
					wm.addView(iconView, iconParams);
					iconViewShow = true;
					loadIconViewData();
				}
				break;
			case 1:
				if (iconViewShow && iconView.getParent() != null) {
					wm.removeView(iconView);
					iconViewShow = false;
				} else if (mainView.getParent() != null) {
					wm.removeView(mainView);
					iconViewShow = false;
					mainViewShow = false;
				}
				break;
			}

		}
	};

	private Handler handler = new Handler();
	private Runnable myRunnable = new Runnable() {
		public void run() {
			Message msg = new Message();
			if (isTopActivity(context)) {
				msg.what = 1;
			} else {
				msg.what = 0;
			}
			floatViewHandler.sendMessage(msg);
			handler.postDelayed(this, 10);
		}
	};

	private Handler songHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			SongMessage songMessage = (SongMessage) msg.obj;
			final SongInfo songInfo = songMessage.getSongInfo();
			switch (songMessage.getType()) {
			case SongMessage.INIT:
				// 本地歌曲
				if (songInfo.getType() == SongInfo.LOCAL) {
					if (iconViewShow) {
						new AsyncTaskHandler() {

							@Override
							protected void onPostExecute(Object result) {
								Bitmap bm = (Bitmap) result;
								if (bm != null) {
									singerPicImageView
											.setBackgroundDrawable(new BitmapDrawable(
													bm));
									// 显示专辑封面图片
								} else {
									bm = MediaUtils.getDefaultArtwork(context,
											false);
									singerPicImageView
											.setBackgroundDrawable(new BitmapDrawable(
													bm));// 显示专辑封面图片
								}
							}

							@Override
							protected Object doInBackground() throws Exception {
								return ImageUtil.getFirstArtwork(
										songInfo.getPath(), songInfo.getSid());
							}
						}.execute();
						timeTextView.setVisibility(View.INVISIBLE);
						playingStatus.setVisibility(View.VISIBLE);
					}
				} else {
					// 网上下载歌曲
				}
				break;
			case SongMessage.LASTPLAYFINISH:
				if (iconViewShow) {
					timeTextView.setText("-00:00");
					timeTextView.setVisibility(View.INVISIBLE);
					playingStatus.setVisibility(View.INVISIBLE);
					Bitmap bm = MediaUtils.getDefaultArtwork(context, false);
					singerPicImageView
							.setBackgroundDrawable(new BitmapDrawable(bm));// 显示专辑封面图片
				}
				if (mainViewShow) {
					wmItemPause.setVisibility(View.INVISIBLE);
					wmItemPlay.setVisibility(View.VISIBLE);
				}
				break;
			case SongMessage.PLAYING:
				if (iconViewShow) {
					if (timeTextView.getVisibility() != View.VISIBLE) {
						timeTextView.setVisibility(View.VISIBLE);
					}
					if (playingStatus.getVisibility() != View.INVISIBLE) {
						playingStatus.setVisibility(View.INVISIBLE);
					}
					timeTextView
							.setText("-"
									+ MediaUtils.formatTime((int) (songInfo
											.getDuration() - songInfo
											.getPlayProgress())));
				}

				if (mainViewShow) {
					if (wmItemPlay.getVisibility() != View.INVISIBLE) {
						wmItemPlay.setVisibility(View.INVISIBLE);
					}
					if (wmItemPause.getVisibility() != View.VISIBLE) {
						wmItemPause.setVisibility(View.VISIBLE);
					}
				}

				break;
			case SongMessage.STOPING:
				timeTextView.setVisibility(View.INVISIBLE);
				playingStatus.setVisibility(View.VISIBLE);
				if (mainViewShow) {
					wmItemPause.setVisibility(View.INVISIBLE);
					wmItemPlay.setVisibility(View.VISIBLE);
				}

				break;
			}
		}

	};

	@Override
	public void onDestroy() {
		handler.removeCallbacks(myRunnable);
		super.onDestroy();
	}

	/**
	 * 判断程序是否在后台运行
	 * 
	 * @param context
	 * @return
	 */
	private boolean isTopActivity(Context context) {
		String packageName = context.getPackageName();
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
		if (tasksInfo.size() > 0) {
			// 应用程序位于堆栈的顶层
			if (packageName.equals(tasksInfo.get(0).topActivity
					.getPackageName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void update(Observable arg0, Object data) {
		if (data instanceof SongMessage) {
			SongMessage songMessage = (SongMessage) data;
			if (songMessage.getType() == SongMessage.INIT
					|| songMessage.getType() == SongMessage.PLAYING
					|| songMessage.getType() == SongMessage.STOPING
					|| songMessage.getType() == SongMessage.ERROR
					|| songMessage.getType() == SongMessage.LASTPLAYFINISH) {
				Message msg = new Message();
				msg.obj = songMessage;
				songHandler.sendMessage(msg);
			}
		}
	}
}
