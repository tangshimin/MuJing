## 幕境
沉浸式学英语,使用自己感兴趣的电影、美剧或文档，生成词库（单词本）。在记忆单词时，可以练习拼写并观看相关的视频片段，以便更好的理解和记忆单词。播放电影时，以弹幕的形式复习词库中的单词，使得单词记忆不再是一件乏味的事。

### [下载地址](https://github.com/tangshimin/MuJing/releases)
## 主要功能：

1. [用 MKV 格式的电影、电视剧生成词库(单词本)](https://github.com/tangshimin/MuJing/wiki#%E5%A6%82%E4%BD%95%E7%94%A8-mkv-%E8%A7%86%E9%A2%91%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)，让每个单词都有具体的语境。

   ![Demo-Generate-Vocabulary-Light](https://user-images.githubusercontent.com/16540656/184311741-15fab9c3-83ba-4080-bac7-ca3a163c67d0.png)

2. [不是 MKV 格式的视频可以使用字幕 + 视频生成词库(单词本)](https://github.com/tangshimin/MuJing/wiki#%E5%A6%82%E4%BD%95%E7%94%A8%E5%AD%97%E5%B9%95%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)

3. 记忆单词，记忆单词的时候，会自动播放单词的读音，然后用键盘打字练习拼写，每个单词都可以输入多次，直到记住为止。从 MKV 生成的词库(单词本)，可以抄写单词对应的字幕，播放单词对应的视频片段。每个单元有 20 个单词，记完一个单元还有听写测试，检查记忆效果。默认使用 Enter 键切换下一个单词。

    https://user-images.githubusercontent.com/16540656/163662386-e82dc534-7a5a-4566-8449-fc71db51f960.mp4
    
    demo 中的电影片段来源于 [Sintel](https://www.youtube.com/watch?v=eRsGyueVLvQ)。
4. 视频播放器，以弹幕的形式复习单词。播放电影时，添加用电影生成的词库到播放器，单词会以弹幕的形式出现。要查看某个单词的中文解释，只需要输入单词或对应的数字就可以查看。打开弹幕的快捷方式：如果正在记忆某个由视频或字幕生成的词库，把视频拖放到记忆单词界面，就可以快速的打开视频和弹幕。

   ![videoPlayer](https://user-images.githubusercontent.com/16540656/220088640-2f9c3a54-500e-477b-8c63-bc31b32d2d71.jpg)

5. 字幕浏览器，可以浏览字幕，练习跟读美剧、电影、TED演讲，可以选择性的播放一条或多条字幕，还可以抄写字幕。如果要播放多行字幕，点击左边的数字就可以开启，点击 5 和 10 再点击左边的播放按钮，
   就会从第5行开始播放，到第10行结束。

   https://user-images.githubusercontent.com/16540656/174944474-e5947df9-c8ed-4546-9c67-057fe52c2d51.mp4

6. 听写复习，可以选择多个章节的单词一起复习，先听写测试，然后再复习错误的单词。

   ![DictionReview](https://user-images.githubusercontent.com/16540656/184179317-f8c0ac99-9048-48da-b59b-5badbaae7c62.png)

7. 抄写文本，可以抄写 [古腾堡计划](https://www.gutenberg.org/) 所有 txt 格式的电子书，非[古腾堡计划](https://www.gutenberg.org/) 的电子书，抄写前可能需要先格式化，把每行的字母数量限制在 75 个以内。

   ![Demo-Text](https://user-images.githubusercontent.com/16540656/175084580-6b26abc3-671f-455e-ac5f-aa583297a0e0.png)

8. [用英文文档生成词库(单词本)](https://github.com/tangshimin/MuJing/wiki#%E5%A6%82%E4%BD%95%E7%94%A8%E6%96%87%E6%A1%A3%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)要读一篇陌生单词比较多的英文文档，又不想一边查词典一边看文档，可以先用文档生成词库，把陌生单词先记一遍，然后看文档的时候会更加流畅。

9. [用 MKV 视频或字幕生成的词库，可以链接到用文档生成的词库或内置的词库](https://github.com/tangshimin/MuJing/wiki#%E9%93%BE%E6%8E%A5%E5%AD%97%E5%B9%95%E8%AF%8D%E5%BA%93)。下面着张图片表示，电影 Sintel 的所有字幕中，有 9 条字幕，匹配了四级词库中的 6 个单词。

   ![Link Vocabulary](https://user-images.githubusercontent.com/16540656/166690274-2075b736-af51-42f0-a881-6535ca11d4d3.png)


## 应用平台：Windows / macOS
### [下载地址](https://github.com/tangshimin/MuJing/releases)

## 开发环境
- UI 框架：[Compose Desktop](https://github.com/JetBrains/compose-jb)
- 开发语言：Kotlin、Java
- OpenJDK:
  > Windows : OpenJDK 17.0.1<br>
  > macOS : OpenJDK 17.0.5
- gradle 7.5.1
- 启动项目之前需要将 `MuJing\resources\common\dictionary` 文件夹里的词典文件`ecdict.mv.db.7z` 解压缩。不然不能使用生成词库功能。打包之前要把`ecdict.mv.db.7z`删掉。
- macOS 系统还需要下载 [VLC 视频播放器](https://www.videolan.org/)， windows 系统也可以下载 VLC，用于调试应用程序，但是程序优先使用项目内置的 VLC DLL,如果内置的 DLL 有问题再试本机安装的 VLC.


## 致谢
感谢 [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner)  的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。

感谢 [skywind3000](https://github.com/skywind3000) 开源 [ECDICT](https://github.com/skywind3000/ECDICT)。

感谢 [libregd](https://github.com/libregd) 为本项目贡献了一些交互设和及非常好的功能建议，以及为 Typing Learner 设计 Logo。

感谢 [网易有道](https://www.youdao.com/) 为本项目提供专业的词典发音。
