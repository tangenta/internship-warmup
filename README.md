# PingCAP-小作业
## 题目要求

```text
有一个 100GB 的文件，里面内容是文本，要求：

1、找出第一个不重复的词
2、只允许扫一遍原文件
3、尽量少的 IO
4、内存限制 16G
```

## 解题思路

首先，把原文件的词及其所在的位置看作一个元组（word, index），逐个读入到内存，按照词的字典序从小到大排序。到达一定大小（小于16G）时，写出到文件，重复执行直到100G文本处理完毕。

将已产生的文件看作有序数组，应用“归并排序”的“归并”一步，即每次从所有数组的首元素中选出最小值，读入内存（该流程记为Read2），进行比对，比对的方法如下：

维护两个元组，结构为（word, index, isDuplicate），其中word表示词本身，index表示该词在原序列中的位置，isDuplicate表示“该词是否已重复出现”。这两个元组共同构成一个状态，随着Read2进行，不断修改状态，最后根据状态得出答案。这一处理过程的规则如下：

- 一个重复词后加一个新词，丢弃重复词。
- 一个独立词后加一个独立词，丢弃原序列位置较大的词。

1. Read2尚未执行时，状态处于初始态，表示为（null, _, _) (null, _, _)。此时如果有新词读入，转到状态2。
2. Read2首次或持续重复读到某个词w1时，状态表示为 (w1, _, true/false) (null, _, _)。此时如果有新词w读入，如果w等于w1，则将第一个元组的重复标记为置为true即可；否则判断w1是否重复，如重复则丢弃，否则转到状态3。
3. 状态3表示为(w1, i1, false) (w2, i2, false/true)。如果新词w3和w2相等，则w2的重复标记置为true；如果不等，新词w3读入时，判断w2是否重复：如果重复，丢弃w2，w3换入w2；如果w2不重复，则丢弃w1和w2位置靠后的词。

当Read2重复执行，直到没有内容可读时，“原序列第一个不重复的词”及其位置已保存在状态中（如果有的话）。

## 示例

原序列：b1 c2 a3 a4 d5 e6 e7 f8 c9

内存大小：3个字符。

排序并写出三个文件

- File1: a3 b1 c2
- File2: a4 d5 e6
- File3: c9 e7 f8

归并，以下是状态变化过程：

1. 初始态：(null, _, _)(null, _, _) 
2. 读入a3：(a, 3, false)(null, _, _)
3. 读入a4：(a, 3, true)(null, _, _)
4. 读入b1：(a, 3, true)(b, 1, false)
5. 读入c2，重复词后得到新词，丢弃重复词：(b, 1, false)(c, 2, false)
6. 读入c9：(b, 1, false)(c, 2, true)
7. 读入d5，重复词后得到新词，丢弃重复词：(b, 1, false)(d, 5, false)
8. 读入e6，两个独立词，保留位置较小的词：(b, 1, false)(e, 6, false)
9. 读入e7：(b, 1, false)(e, 6, true)
10. 读入f8，重复词后得到新词，丢弃重复词：(b, 1, false)(f, 8, false)

最后选择根据状态判断第一个不重复的词：b

## 目录结构

```
.
├── pom.xml
├── README.md
├── resources
│   ├── integ-test.txt
│   └── scanner-test.txt
├── src
│   ├── main
│   │   └── java
│   │       └── com
│   │           └── tangenta
│   │               ├── App.java				  # 主程序，定义了两个关键过程
│   │               ├── data					  # 数据结构
│   │               │   ├── Tuple.java
│   │               │   └── WordPosition.java	  # （词，位置）
│   │               └── scanner
│   │                   ├── MergeScanner.java     # 归并多个扫描器
│   │                   ├── Scanner.java
│   │                   ├── TextScanner.java      # 扫描原文件
│   │                   └── WordPosScanner.java   # 扫描保存（词，位置）的文件
│   └── test
│       └── java
│           └── com
│               └── tangenta
│                   └── AppTest.java
```

## API

```java
public static Optional<WordPosition> findFirstNonDup(Path textFile, Path tempDir, long flushLimitSizeInBytes)
```

App.java静态方法findFirstNonDup接受文件路径，临时文件目录和冲刷阈值（字节），返回第一个词及其位置，如果不存在这样的词，返回Optional.empty()。

## 优化措施

1. 从存储介质读取数据时，每次读的量都只有若干个字节，如果每次都触发system call，context switch开销较大，因此可以缓存，一次读入一“块”数据。
2. 文件的写入读出利用gzip流进行压缩和解压，减小IO量。
3. 排序时，过滤已有的重复词，减小IO量。

## IO量分析

- 总读入量 = 原文件读入100G + 第二阶段读入100G + 每个词位置信息8个字节*（词总数 - 去重词数 ） - 压缩量 - 去重词量

- 总写出量 = 写出100G + 每个词位置信息8个字节*（词总数 - 去重词数 ） - 压缩量 - 去重词量