# ADQL Formal Grammar Specification

> **ADQL** (App Debloating Query Language)  
> 用于描述安卓应用降肿场景的 DSL

---

## 1. 文法四元组

G = (T, N, P, S)

- **T**：终结符（Tokens）  
- **N**：非终结符（Non-terminals）  
- **P**：产生式（Productions）  
- **S**：起始符号（Start symbol）

---

## 2. 起始符号

S = <query_list>

---

## 3. 终结符 T

| 符号                | 说明                                    |
|---------------------|-----------------------------------------|
| `'FLOW'`            | 数据流切分关键词                        |
| `'FROM'`, `'TO'`    | 数据流起止标记                          |
| `'KEEP_ACTIVITIES'` | 保留 Activity 列表                      |
| `'REMOVE_ACTIVITIES'`| 排除 Activity 列表                     |
| `'KEEP_PERMISSIONS'`| 保留 Permission 列表                    |
| `'KEEP_LIBRARIES'`  | 保留 库函数 列表                        |
| `'KEEP_SOURCE'`     | 保留 单一 Source 点                     |
| `'USES'`            | 附加元信息标记                          |
| `'Class'`, `'Method'`, `'Statement'` | 引用元素类型                |
| `'['`, `']'`, `','`  | 列表分隔符                              |
| `'('`, `')'`        | 元素参数括号                            |
| `'\'' ... '\''`     | 字符串字面量（单引号包裹）              |
| `'?'`               | 每条查询语句结束标志                    |
| **其它标识符**      | 允许出现的标识符字符（字母、数字、`.`、`_`） |

---

## 4. 非终结符 N

<query_list>, <query>, <flow_query>, <operation_query>, <operation_type>, <element_type>, <source_element>, <sink_element>, <element_list>, <string_list>, <string_literal>


---

## 5. 产生式 P

```bnf
<query_list>       → ( <query> '?' )* EOF

<query>            → <flow_query> 
                   | <operation_query>

<flow_query>       → 'FLOW' 'FROM' <source_element>  
                     'TO' <sink_element>

<operation_query>  → <operation_type> <element_list> [ 'USES' <string_list> ]
                   | 'KEEP_SOURCE' <source_element> [ 'USES' <string_list> ]

<operation_type>   → 'KEEP_ACTIVITIES'
                   | 'REMOVE_ACTIVITIES'
                   | 'KEEP_PERMISSIONS'
                   | 'KEEP_LIBRARIES'

<element_type>     → 'Method'
                   | 'Class'
                   | 'Statement'

<source_element>   → <element_type> '(' <string_literal> ')'

<sink_element>     → <element_type> '(' <string_literal> ')'

<element_list>    → '[' <string_list> ']'

<string_list>      → <string_literal> ( ',' <string_literal> )*

<string_literal>   → '\'' <any character except '\''>* '\''
```

## 6. 实例

```adql
FLOW
  FROM Method('com.example.DataCollector.getData')
  TO   Method('com.example.ReportSender.send') ?

KEEP_ACTIVITIES ['com.example.MainActivity', 'com.example.LoginActivity'] 
  USES ['com.example.Analytics.trackEvent'] ?

KEEP_SOURCE Method('com.example.MyClass.init') ?
```






