import sys
import zhipuai
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QTextEdit,
    QPushButton, QLabel, QMessageBox
)

# 设置你的 ZhipuAI API Key
zhipuai.api_key = "05100a8caee0d62f5f27b7d7002ecb9a.6Rs0e9GjII2pIBnJ"

class FlowSlicerGUI(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("FlowSlicer")
        self.setGeometry(100, 100, 800, 600)

        layout = QVBoxLayout()

        # 用户输入
        self.input_label = QLabel("请输入您的降肿需求（自然语言）:")
        self.input_text = QTextEdit()
        layout.addWidget(self.input_label)
        layout.addWidget(self.input_text)

        # 按钮
        self.send_button = QPushButton("发送给LLM（ZhipuAI）")
        self.send_button.clicked.connect(self.query_llm)
        layout.addWidget(self.send_button)

        # LLM输出的DSL
        self.dsl_label = QLabel("LLM生成的DSL脚本：")
        self.dsl_text = QTextEdit()
        layout.addWidget(self.dsl_label)
        layout.addWidget(self.dsl_text)

        # 执行DSL
        self.exec_button = QPushButton("执行DSL")
        self.exec_button.clicked.connect(self.execute_dsl)
        layout.addWidget(self.exec_button)

        # 结果输出
        self.output_label = QLabel("执行结果：")
        self.output_text = QTextEdit()
        self.output_text.setReadOnly(True)
        layout.addWidget(self.output_label)
        layout.addWidget(self.output_text)

        self.setLayout(layout)

    def query_llm(self):
        user_input = self.input_text.toPlainText().strip()
        if not user_input:
            QMessageBox.warning(self, "错误", "请输入降肿需求")
            return

        try:
            response = zhipuai.model_api.invoke(
                model="glm-4-long",
                messages=[
                    {"role": "system", "content": "你是一个将自然语言需求转化为DSL脚本的助手。"},
                    {"role": "user", "content": user_input}
                ],
                temperature=0.5,
                top_p=0.7
            )
            dsl_script = response["data"]["choices"][0]["content"]
            self.dsl_text.setPlainText(dsl_script)

        except Exception as e:
            QMessageBox.critical(self, "ZhipuAI API错误", str(e))

    def execute_dsl(self):
        dsl = self.dsl_text.toPlainText().strip()
        if not dsl:
            QMessageBox.warning(self, "错误", "没有可执行的DSL脚本")
            return

        result = self.call_dsl_executor(dsl)
        self.output_text.setPlainText(result)

    def call_dsl_executor(self, dsl_script: str) -> str:
        # TODO: 替换为你的 DSL 解析器调用逻辑
        try:
            from your_dsl_executor_module import parse_and_run_dsl
            return parse_and_run_dsl(dsl_script)
        except Exception as e:
            return f"DSL执行出错：{str(e)}"

if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = FlowSlicerGUI()
    window.show()
    sys.exit(app.exec_())
