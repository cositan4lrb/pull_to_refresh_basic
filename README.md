##pull_to_refresh_basic 使用指南    

#项目简介

**pull_to_refresh_basic**是[大胖](http://www.dapang.cc)用Gradle发布项目到JCenter的一个尝试。项目本身是实现了许多社交类应用必备的「下拉刷新」以及「加载更多」功能。目的主要是为了演示实现方法，梳理思路，给深入开发以启迪。 

###引用方法
    
**Android Studio用户**            

在你的 build.gradle 文件中添加如下 compile 命令即可引用我的项目。

  `compile 'com.hustdapang.pulltorefreshbasic:app:0.13'`  
    
    
#文档风格 1.0   
    
###文字   
1. 英文与非标点的中文之间需要有一个空格，如「刘 dapang 爱琰琰」而不是「刘dapang爱琰琰」。   
2. 文档尽量使用中文，避免中英文混合的情况。例如「app」一般应写为「应用」或「移动应用」，以避免由于英文译法不同造成混淆。  

###标点   
1. 中文标点与其他字符间一律不加空格。    
2. 中文文案中使用中文方引号「」。  

###段落   
1. 如果是纯文本，段落之间使用一个空行隔开。如果是 HTML 或其他富文本格式，使用额外空白作为段落间的分隔。    
2. 段落开头不要留出空白字符。

    
#项目详述  

###下拉刷新

-  **初始化头布局**            
    
    `private void initHeader() {...}`
        
     设置 padding 隐藏头布局，调用下拉刷新动画方法 *initAnimation()* 。    

- **下拉刷新动画**    
    
    `private void initAnimation() {...}`    
    
    实现下拉刷新的「箭头旋转」和「圆圈转动」的效果。    

- **触摸监听**  

    `public boolean onTouchEvent(MotionEvent ev) {...}` 

    对于双头布局采取了优化。因为实际情况下 ListView 不会只是单纯的 List，更多的可能是包含一个图片展示（ImageView）或者置顶控件。因此特意以一个 ImageView 为例，演示如何进行多重判断。  

- **下拉刷新事件处理**  
    
    `public void setOnRefreshListener(OnRefreshListener listener) {...}`    
    
    &
    
    `public interface OnRefreshListener {...}`
    
    为触摸监听提供事件处理方法。      
    
    主要实现了双头布局时的触摸事件处理。当用户自定义头布局没有完全显示时，屏蔽下拉刷新操作。
    实现当前状态确认，避免不必要的网络请求。     
    

    
 
    
- **自定义添加头布局**

    `public void addCustomHeaderView(View v) {...}` 
    
    将 ListView 的 *addHeaderView()* 方法重写，实现用户自定义加入额外头布局的功能。

###加载更多
    
- **初始化脚布局**    
    
    `private void initFooter(){...}`    
    
    包含加入到 ListView 的 *addFooterView()* 原生方法。并设置了滑动监听 *OnScrollListener* 。
    
- **页面滑动状态判断**  

    `public void onScrollStateChanged(AbsListView view, int scrollState){...}`
    
    滑动监听的事件处理。包含判断是否到底部以及是否处于「加载更多状态」。处理后可以减少网络请求，避免不必要的流量浪费。
    
    
###Others   
    
- **配置是否启用下拉刷新和加载更多**       
    
    `public void isEnabledPullDownRefresh(boolean isEnabled) {...} `
    
    &   
        
    `public void isEnabledLoadingMore(boolean isEnabled) {...}` 
    
    传入布尔类型的参数。  
    
- **下拉刷新/加载更多完成后自动隐藏布局**    

    `public void onRefreshFinish(){...}`    
    
    当网络请求结束后，可以调用本方法，终止动画。  
        
- **获取当前时间**    
    
    `private String getCurrentTime(){...}`  
    
    调用的是 Java 的 API *SimpleDateFormat* 具体配置参数用户可以自行查阅。默认的是 *年月日 时（24h）分秒*。

    用于在下拉刷新时同步显示当前时间。   
    
    
#反馈 
    
如果有任何问题或建议，请联系我。
    
- Wechat: Lonely4God  
- Email: cositan4lrb@foxmail.com 
- Weibo: [http://weibo.com/cositan4lrb](http://weibo.com/cositan4lrb ) 
    
