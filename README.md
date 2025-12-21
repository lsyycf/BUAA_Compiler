# 编译器设计文档

## 1. 参考编译器介绍

在本项目开始之前，深入阅读并分析了一份基于 Java 实现的参考编译器源代码。该参考编译器采用了一种典型的分层架构，具有较高的模块化程度，代码组织清晰，逻辑严密。

### 1.1 总体结构

参考编译器采用了 **前端 -> 中端 -> 后端** 的经典三层架构，各层职责划分明确，通过定义良好的中间数据结构进行耦合。

#### 1.1.1 前端

前端主要负责对源代码进行解析，进行词法分析、语法分析以及语义分析的初步处理。

- **词法分析**：`SourceFileLexer` 读取源文件内容，`TokenLexer` 根据词法规则将字符流转换为 `Token` 流。每个 `Token`
  包含类别和字面值。
- **语法分析**：采用递归下降分析法。以 `CompUnitParser` 为入口，根据 SysY 语言文法，自顶向下地构建语法树。语法树的节点由
  `SyntaxNode` 的子类构成，涵盖了声明、函数定义、语句、表达式等语法成分。
- **错误处理**：在解析过程中，前端还负责识别并记录语法错误，通过 `ErrorTable` 进行统一管理。

#### 1.1.2 中端

这是参考编译器的核心部分，负责将前端生成的语法结构转换为 LLVM IR。

- **LLVM IR 生成**：`IrBuilder` 通过遍历前端生成的语法树，构建出对应的中间代码 `IrModule`。
- **User-Use-Value 设计模式**：参考了 LLVM 的核心设计，所有的 IR 节点均继承自 `IrValue`。
    - `IrValue`：代表一个有类型的值，可以被其他指令使用。
    - `IrUser`：继承自 `IrValue`，代表使用其他 `IrValue` 作为操作数的指令。
    - `IrUse`：维护了 `IrUser` 和 `IrValue` 之间的引用关系，为后续可能的 SSA构建和优化提供了基础数据结构支持。
- **符号表管理**：`SymbolTable` 及其子类用于在中间代码生成阶段管理变量和函数的作用域、类型及对应的 IR 值，支持块级作用域的嵌套。

#### 1.1.3 后端

后端负责将与机器无关的 LLVM IR 转换为特定的 MIPS 汇编代码。

- **指令映射**：`MipsBuilder` 接收 `IrModule`，将其中的 `IrFunction`、`IrBasicBlock` 和 `IrInstruction` 逐一翻译为 MIPS
  架构下的 `MipsFunction`、`MipsBasicBlock` 和 `MipsInstruction`。
- **指令集支持**：实现了常用的 MIPS 指令类，包括算术运算、逻辑运算、分支跳转、内存访问以及系统调用。
- **寄存器与栈管理**：`RegisterFile` 负责管理 MIPS 寄存器。后端处理了从虚拟寄存器到物理寄存器或栈偏移量的映射，确保生成的汇编代码能够正确执行函数调用和参数传递。

### 1.2 接口设计

各层之间通过明确定义的接口类和数据结构进行交互，降低了模块间的耦合度。

- **Lexer 与 Parser**：
    - 交互媒介：`TokenList` 和 `TokenListIterator`。
    - 机制：Lexer 一次性生成完整的 `Token` 列表。Parser 通过迭代器消费 Token，利用 `peek()` 预读 Token 类型以决定分支预测，利用
      `next()` 消耗 Token 并构建具体的 `SyntaxNode`。
- **Parser 与 IR**：
    - 交互媒介：`IrBuilder` 与 `SyntaxNode`。
    - 机制：`IrBuilder` 不直接作为 Parser 的一部分，而是作为一个独立的构建器。它通过递归访问语法树的根节点，在访问过程中维护当前的
      `IrBasicBlock` 和 `SymbolTable` 状态。
    - 关键转换：例如，当访问 `StmtReturn` 节点时，`IrBuilder` 会生成 `IrRet` 指令并将其插入当前的基本块中；访问 `LVal`
      时，会从符号表中查找对应的指针地址并生成 `IrLoad`或返回地址。
- **IR 与 Backend**：
    - 交互媒介：`IrModule` 与 `MipsBuilder`。
    - 机制：`MipsBuilder` 接收完整构建好的 `IrModule`。它通过三层循环遍历整个 IR 结构：
        1. 遍历 `IrModule` 中的全局变量和函数。
        2. 遍历 `IrFunction` 中的 `IrBasicBlock`。
        3. 遍历 `IrBasicBlock` 中的 `IrInstruction`。
    - 转换逻辑：针对每一条 IR 指令，后端生成对应的 MIPS 指令序列。

### 1.3 文件组织

参考编译器的文件组织非常细致，严格按照架构分包，特别是 AST 节点和 IR 指令的分类，体现了面向对象的设计思想。

```
src/
├── Compiler.java                     // [入口] 编译器主程序，串联各阶段
├── frontend/                         // [前端] 词法与语法分析
│   ├── SourceFileLexer.java          // 源文件读取与处理
│   ├── lexer/                        // 词法分析器相关
│   │   ├── Token.java                // Token 定义
│   │   ├── TokenLexer.java           // 核心词法分析逻辑
│   │   ├── TokenList.java            // Token 列表容器
│   │   └── TokenType.java            // Token 类型枚举
│   └── parser/                       // 语法分析器与 AST 节点
│       ├── CompUnitParser.java       // 解析器入口
│       ├── SyntaxNode.java           // AST 节点基类
│       ├── declaration/              // 变量与常量声明节点 (ConstDecl, VarDecl...)
│       ├── expression/               // 表达式节点 (AddExp, MulExp, LVal...)
│       ├── function/                 // 函数定义节点 (FuncDef, MainFuncDef...)
│       ├── statement/                // 语句节点 (StmtIf, StmtWhile, Block...)
│       └── terminal/                 // 终结符节点 (Ident, IntConst...)
├── middle/                           // [中端] LLVM IR 与符号表
│   ├── error/                        // 错误处理 (ErrorTable)
│   ├── symbol/                       // 符号表系统
│   │   ├── SymbolTable.java          // 符号表管理
│   │   ├── SymbolVar.java            // 变量符号
│   │   └── SymbolFunc.java           // 函数符号
│   └── llvmir/                       // LLVM IR 中间表示
│       ├── IrBuilder.java            // IR 构建器 (AST -> IR)
│       ├── IrModule.java             // IR 顶层模块
│       ├── IrValue.java              // [核心] 所有 IR 对象的基类
│       ├── IrUser.java               // [核心] 使用者基类
│       ├── IrUse.java                // [核心] 使用关系 (Def-Use)
│       ├── type/                     // IR 类型系统 (IrIntegerType, IrPointerType...)
│       └── value/                    // IR 具体值与指令
│           ├── basicblock/           // 基本块 (IrBasicBlock)
│           ├── constant/             // 常量 (IrConstantInt, IrConstantArray)
│           ├── function/             // 函数 (IrFunction, IrParam)
│           ├── globalvariable/       // 全局变量
│           └── instructions/         // IR 指令集
│               ├── IrBinaryInst.java // 二元运算指令
│               ├── memory/           // 内存操作 (IrAlloca, IrLoad, IrStore...)
│               ├── terminator/       // 终结指令 (IrBr, IrRet, IrCall...)
│               └── IrLabel.java      // 标签指令
└── backend/                          // [后端] MIPS 代码生成
    ├── MipsBuilder.java              // MIPS 构建器 (IR -> MIPS)
    ├── MipsModule.java               // MIPS 顶层模块
    ├── RegisterFile.java             // 寄存器文件定义
    ├── basicblock/                   // MIPS 基本块
    ├── function/                     // MIPS 函数
    ├── symbol/                       // 后端符号表
    └── instruction/                  // MIPS 指令集
        ├── MipsInstruction.java      // MIPS 指令基类
        ├── Add.java, Sub.java...     // 算术指令
        ├── Lw.java, Sw.java...       // 访存指令
        ├── Beq.java, J.java...       // 跳转指令
        └── Syscall.java              // 系统调用
```

## 2. 编译器总体设计

基于对参考编译器的深入学习与分析，本项目旨在实现一个架构清晰、功能完备的 SysY 语言到 MIPS
汇编的编译器。在总体设计上，本项目吸取了分层解耦的优秀思想，并在中间表示（IR）的数据结构设计上进行了针对性的简化和改良，采用更线性的四元式结构，便于快速开发，也能提升基于基本块的优化效率。

### 2.1 总体架构

本项目采用了经典的四段式流水线结构：**Frontend -> IR Generator -> Optimizer -> Mips Generator **。各阶段职责明确，耦合度低。

#### 2.1.1 前端

- **词法分析**：基于正则表达式读取源代码字符流，识别关键字、标识符、整数常量、运算符等，去除注释，生成 `TokenList` 记号序列。
- **语法分析**：采用递归下降分析法，根据 SysY 语法规则处理 `TokenList`，构建以 `CompUnit`
  为根节点的抽象语法树（AST）。在此过程中包含错误恢复机制，能够处理缺失分号、括号不匹配等语法错误。
- **语义分析**：遍历 AST，构建符号表，处理作用域嵌套，进行类型检查、常量计算及参数匹配检查。

#### 2.1.2 中间代码

- 不同于参考编译器中复杂的对象图结构，本项目采用了更为扁平化、线性的 **四元式** 结构。
- 通过遍历 AST 并结合符号表信息，生成中间代码指令列表。
- 这种结构天然契合指令流的顺序执行特性，便于后续的扫描和基本块划分。支持全局变量与局部变量的区分处理，以及数组的地址计算。

#### 2.1.3 优化

- 位于 `optimize` 包中，是本项目的一大亮点，它建立在 **控制流图** 的基础之上。
- **基本块构建**：将线性的四元式序列切割为 `Block`（基本块）和 `Func`（函数对象）。
- **多趟优化 Pass**：实现了死代码消除、常量传播、循环不变式外提、公共子表达式消除、强度削弱以及窥孔优化等多种优化策略。

#### 2.1.4 后端

- `MipsGenerator` 负责将优化后的中间代码翻译为 MIPS32 汇编指令。
- 引入 `FuncStack` 类进行函数栈帧管理，负责计算局部变量和参数在栈上的偏移量，管理 `$fp`、`$sp` 及 `$ra` 寄存器，确保函数调用约定的正确性。

### 2.2 接口设计

各模块之间通过定义良好的数据结构进行交互，实现了高内聚低耦合：

- **Lexer -> Parser**：通过 `TokenList` 传递 Token 序列。`Token` 包含类别、行号及原始字符串信息，支持 `peek` 和 `consume`
  操作。
- **Parser -> AST**：解析器自顶向下构建 AST 对象树。AST 节点类位于 `frontend.element` 包中，不仅存储语法结构，还封装了
  `toString` 方法以便于输出调试。
- **AST -> IR**：`IrGenerator` 通过访问者模式或直接遍历 AST 节点，生成 `IrList`。`IrList` 内部维护一个
  `ArrayList<Quadruple>`，每个 `Quadruple` 包含 `op`, `arg1`, `arg2`, `result` 四个字段。
- **IR -> Optimizer**：优化器接收原始的 `IrList`，将其转换为 `ArrayList<Func>` 和 `ArrayList<Block>` 的图结构进行处理，最终展平回优化后的
  `IrList`。
- **IR -> Mips**：`MipsGenerator` 读取最终的 `IrList`，结合符号表信息和 `FuncStack` 维护的栈帧布局，输出符合 MARS 模拟器标准的
  MIPS 汇编字符串。

### 2.3 文件组织

项目代码结构清晰，严格按照功能模块划分包结构：

```
src
├── Compiler.java                 // 编译器主入口，负责串联各阶段流水线
├── config.json                   // 编译器配置文件
├── frontend                      // 前端子系统
│   ├── config
│   │   ├── ErrorType.java        // 错误类型定义 (枚举)
│   │   ├── SymbolType.java       // 符号类型定义 (枚举)
│   │   └── TokenType.java        // Token 类型定义 (正则表达式映射)
│   ├── data
│   │   ├── ErrorList.java        // 错误收集容器
│   │   ├── SymbolMap.java        // 单层作用域符号表
│   │   ├── SymbolTree.java       // 树状符号表，处理作用域嵌套
│   │   └── TokenList.java        // Token 序列容器
│   ├── element                   // AST 节点类定义 (承载语法与语义信息)
│   │   ├── CompUnit.java         // 编译单元 (AST 根节点)
│   │   ├── Decl.java             // 声明节点
│   │   ├── FuncDef.java          // 函数定义节点
│   │   ├── Stmt.java             // 语句节点
│   │   ├── Exp.java              // 表达式节点
│   │   └── ... (其他 AST 节点: Block, Cond, LVal 等)
│   ├── lexer
│   │   ├── Lexer.java            // 词法分析器核心逻辑
│   │   ├── Token.java            // Token 实体类
│   │   └── Reader.java           // 字符流读取辅助类
│   ├── parser
│   │   ├── Parser.java           // 语法分析器核心逻辑 (递归下降)
│   │   └── Reader.java           // Token 流读取辅助类
│   ├── symbol
│   │   ├── Symbol.java           // 符号实体类
│   │   └── Visitor.java          // 语义分析访问者 (构建符号表、类型检查)
│   └── utils
│       ├── FileIO.java           // 文件读写工具
│       └── ToString.java         // 格式化输出工具
├── backend                       // 后端子系统
│   ├── data
│   │   ├── IrList.java           // 中间代码列表容器
│   │   ├── Quadruple.java        // 四元式核心数据结构 (op, arg1, arg2, result)
│   │   └── FuncStack.java        // 函数栈帧管理器
│   ├── ir
│   │   └── IrGenerator.java      // 中间代码生成器
│   ├── mips
│   │   └── MipsGenerator.java    // MIPS 汇编生成器
│   └── utils
│       └── Calculate.java        // 编译期计算辅助工具
└── optimize                      // 优化子系统
  ├── Optimize.java             // 优化器入口与调度
  ├── Func.java                 // 函数级优化 (CFG构建、数据流分析)
  └── Block.java                // 基本块级优化 (DAG构建、指令管理)
```

## 3. 词法分析设计

### 3.1 编码前的设计

在设计之初，词法分析器的目标是将源代码的字符流转换为 Token 流。为了简化实现并提高可维护性，决定采用 Java 的
`java.util.regex` 正则表达式库来识别各类 Token。

- **Token 定义**：在 `TokenType` 枚举中定义所有可能的 Token 类型，并将每个类型与一个对应的正则表达式绑定。
- **读取策略**：设计一个 `Reader` 类，负责按行读取源代码，维护当前的行号和列号，并提供 `peek` 和 `consume` 接口供 Lexer 使用。
- **匹配逻辑**：Lexer 依次尝试匹配 `TokenType` 中的正则模式，一旦匹配成功，即生成一个 `Token` 对象并存入 `TokenList`，同时推进
  `Reader` 指针。

### 3.2 编码完成之后的修改

在实际编码过程中，遇到了一些特殊情况，导致了对原设计的微调：

- **注释处理**：最初未充分考虑块注释 `/* ... */` 的跨行处理。修改后的 `Lexer.java` 中增加了专门的 `isComment()`
  方法，能够识别并跳过单行注释 `//` 和多行注释 `/* ... */`，确保生成的 Token 流纯净。
- **错误处理**：增加了对非法字符的检测机制。如果当前字符无法匹配任何定义的 Token 模式，将其标记为 `ILLEGAL_CHAR` 错误（错误码
  `a`），并在错误列表中记录。特别处理了如 `|` 和 `&` 这类非标准单字符运算符，尝试向后窥探以进行容错。
- **贪婪匹配优化**：为了避免标识符前缀匹配问题（如 `int` 匹配为 `int` 关键字而非标识符），调整了正则顺序和匹配逻辑，确保关键字优先匹配，且匹配时利用正则边界符。

## 4. 语法分析设计

### 4.1 编码前的设计

语法分析器采用标准的 **递归下降分析法**。

- **AST 构建**：为文法中的每一个非终结符设计对应的 AST 节点类，位于 `frontend.element` 包中。
- **解析流程**：Parser 类持有 `TokenList` 的读取器，每个解析方法对应一个非终结符。方法内部根据当前 Token
  的类型决定进入哪个产生式的分支，并递归调用其他解析方法。
- **接口统一**：所有 AST 节点重写 `toString` 方法，按照作业要求的格式输出，便于调试和评测。

### 4.2 编码完成之后的修改

编码过程中主要解决文法二义性和错误恢复的问题：

- **Stmt 的二义性消除**：在解析 `Stmt` 时，`LVal = Exp;` 和 `Exp;` 存在 First 集冲突。为此引入了 `getStmtType()`
  方法，通过向前窥探（peek）多个 Token 来寻找赋值号 `=`，从而准确区分是赋值语句还是表达式语句。
- **错误恢复机制**：针对 SysY 定义的三类语法错误（缺失分号 、缺失右小括号、缺失右中括号），在 `Parser` 中实现了 `consumeError`
  方法。当预期符号未出现时，不是立即抛出异常终止，而是记录错误并继续解析后续内容，极大地增强了编译器的鲁棒性。
- **左递归与优先级**：表达式解析（`Exp` -> `AddExp` -> `MulExp` ...）采用层级调用的方式隐含处理了运算符优先级，避免了左递归导致的无限循环。

## 5. 语义分析设计

### 5.1 编码前的设计

语义分析的核心是符号表的构建和类型检查。

- **Visitor 模式**：设计 `Visitor` 类遍历 AST，在遍历过程中建立符号表并进行语义检查。
- **符号表结构**：采用树状结构的符号表 `SymbolTree`。每个 `SymbolTree` 节点代表一个作用域，包含一个 `SymbolMap`
  ，用于存储当前作用域的符号，以及指向父级 `SymbolTree` 的指针。
- **符号定义**：`Symbol` 类存储变量/函数的名称、类型、所在行号以及维数信息。

### 5.2 编码完成之后的修改

为了支持更复杂的语义检查和后续的代码生成，进行了以下增强：

- **常量计算**：在处理数组定义（如 `int a[m]`）时，必须在编译期计算出数组大小。因此实现了 `evaConstExp`
  等系列方法，能够在语义分析阶段直接计算出常量表达式的值。
- **作用域链接**：在 `SymbolTree` 中增加了 `subTree` 列表和 `symbolTreeMap` 映射，能够通过 ID 快速定位到具体的子作用域，为
  IR 生成阶段提供便利。
- **循环控制检查**：为了处理 `break` 和 `continue` 语句出现在非循环块中的错误，在 Visitor 中引入了 `loopDeep` 计数器。进入循环时
  `loopDeep++`，退出时 `loopDeep--`，若在 `loopDeep == 0` 时遇到跳转语句则报错。
- **参数匹配**：完善了函数调用时的实参形参匹配逻辑，不仅检查参数个数，还严格检查参数类型。

## 6. 代码生成设计

### 6.1 编码前的设计

代码生成分为两个阶段：AST 到 IR，IR 到 MIPS。

- **中间代码 (IR)**：最初考虑使用树形 IR，但为了方便后续的线性扫描和优化，最终决定设计一种扁平化的四元式 IR。定义了
  `Quadruple` 类，包含 `op`, `arg1`, `arg2`, `result`。
- **MIPS 生成**：设计 `MipsGenerator` 类，读取 IR 列表，通过简单的模板匹配将每条 IR 翻译为对应的 MIPS 指令。

### 6.2 编码完成之后的修改

- **IR 命名规范**：为了处理不同作用域下的同名变量，在生成 IR 时采用了重命名策略。变量名格式化为
  `ir_idenfr_{name}_{scopeId}`，确保了全局唯一的变量名，避免了名字冲突。
- **短路求值**：在生成逻辑表达式的代码时，采用了短路跳转机制。例如 `LAndExp` 中，如果左侧为假，直接跳转到 False 标签，不再计算右侧。
- **FuncStack 栈管理**：在 MIPS 生成阶段，引入了 `FuncStack` 类来精细管理函数栈帧。它负责记录局部变量和参数相对于 `$fp`
  的偏移量，动态计算栈空间大小，并处理函数调用时的参数压栈和栈平衡（`subu $sp` / `addu $sp`）。
- **全局数据区**：分离了 `.data` 和 `.text` 段的生成。`MipsGenerator` 先扫描一遍 IR 收集所有的全局变量和字符串常量，统一生成
  `.data` 段，然后再生成代码段。

## 7. 代码优化设计

### 7.1 编码前的设计

最初的优化设计仅限于简单的窥孔优化，例如删除显而易见的冗余指令（如 `a = a`）或不可达代码。

### 7.2 编码完成之后的修改

为了追求更高的性能，本项目引入了基于 **控制流图** 的高级优化框架，位于 `src/optimize` 包中。

- **基本块与流图构建**：
    - 实现了 `Func` 和 `Block` 类。`Func` 将指令序列根据跳转指令和标签切割为多个基本块 `Block`。
    - 通过分析块间的跳转关系，构建了前驱和后继关系，形成了完整的 CFG。
- **实现的优化 Pass**：
    1. **全局常量传播**：基于数据流分析，在 CFG 上迭代计算变量的常量值，将可计算的表达式直接替换为常量结果。
    2. **局部公共子表达式消除**：在基本块内部构建 DAG，识别并重用重复计算的节点，减少冗余运算。
    3. **死代码消除**：基于活跃变量分析，迭代计算 `in` 和 `out` 集合，删除那些定义了但从未被使用的变量赋值指令。
    4. **循环不变式外提**：实现了支配树构建和自然循环识别，识别循环内部不随循环迭代改变的计算，将其提升到循环的前置首部中执行。
    5. **强度削弱 **：将乘法和除法运算优化为代价更低的移位运算。例如，`x * 4` 优化为 `x << 2`，并处理了除法优化中的符号位调整细节。
    6. **窥孔优化**：除了基础的冗余消除，还增加了对连续跳转的优化。

通过这些多层次的优化策略，编译器生成的汇编代码在执行效率上有了显著提升。