PPVOD 文件上传SDK
--------------------------------
## 构建
项目是标准的maven结构,通过mvn构建
## 使用说明
```java
// 标准上传
//第一个参数 上传url
//第二个参数apikey
//第三个是分片大小,这里填写的是20MB
Uploader up = new Uploader("http://localhost:2100/uploads/", "v1", 20 * 1024 * 1024);
JobFuture future = up.upload("d:\\wfsroot\\somebody.mp4");

//这个上传时立即返回的,如果中途想取消上传,可以执行
//future.cancel();
//通过轮训 future.getStatue(),查询任务的状态
Thread.sleep(3000);
System.out.println(future.getStatue());

// 第二次上传会有秒传效果
future = up.upload("d:\\wfsroot\\somebody.mp4");
Thread.sleep(3000);
System.out.println(future.getStatue());
```
