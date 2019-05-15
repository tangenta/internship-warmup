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

维护两个元组，结构为（word, index, isDuplicate），其中word表示词本身，index表示该词在原序列中的位置，isDuplicate表示“该词是否已重复出现”。这两个元组共同构成一个状态，随着Read2进行，不断修改状态，最后根据状态得出答案。这一处理过程可能出现的状态共有5种：

| 编号 | 状态（"_"表示不关心该值）       | 解释                    | Read2得到的下一个词是w                                       |
| ---- | ------------------------------- | ----------------------- | ------------------------------------------------------------ |
| 0    | (null, _, _) (null, _, _)       | 初始状态，Read2尚未执行 | w写到tuple1，转到状态1                                       |
| 1    | (w1, _, false) (null, _, _)     | 只有一个独立词          | 如果w等于w1，转到状态2，否则转到状态3                        |
| 2    | (w1, _, true) (null, _, _)      | 只有一个重复词          | 如果w等于w1，留在状态2，否则丢弃w1重复词，转到状态1          |
| 3    | (w1, i1, false) (w2, i2, false) | 有两个独立词            | 如果w等于w2，转到状态4，否则只留下位置靠前的独立词，留在状态3 |
| 4    | (w1, _, false) (w2, _, true)    | 独立词后面有一个重复词  | 如果w等于w2，留在状态4，否则丢弃w2重复词，转到状态3          |

当Read2重复执行，直到没有内容可读时，“原序列第一个不重复的词”及其位置已保存在状态中（如果有的话）。

## 目录结构

```
.
├── PingCAP-homework.iml
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
│   │               │   ├── State.java
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

## 优化措施

1. 从存储介质读取数据时，每次读的量都只有若干个字节，如果每次都触发system call，context switch开销较大，因此可以缓存，一次读入一“块”数据。
2. 文件的写入读出利用gzip流进行压缩和解压，减小IO量。
3. 排序时，过滤两个以上的相同词，减小IO量。

## IO量分析

- 总读入量 = 原文件读入100G + 第二阶段读入100G + 每个词位置信息8个字节*（词总数 - 去重词数 ） - 压缩量 - 去重词量

- 总写出量 = 写出100G + 每个词位置信息8个字节*（词总数 - 去重词数 ） - 压缩量 - 去重词量