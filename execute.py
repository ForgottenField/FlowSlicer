import tkinter as tk
from tkinter import filedialog, messagebox, scrolledtext
import requests
import os
import shutil
import subprocess

ZHIPU_API_KEY = os.getenv("ZHIPU_API_KEY") or "05100a8caee0d62f5f27b7d7002ecb9a.6Rs0e9GjII2pIBnJ"
ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
TARGET_DIR = r"D:\ISCAS\myTools\FlowSlicer_with_experiments\FlowSlicer\apk"

class LLMDialog:
    def __init__(self):
        self.history = [{"role": "system", "content": "你是一个 ADQL 编写助手。用户将用自然语言描述需求，你需要返回符合语法的 ADQL DSL 代码。"}]

    def add_user_input(self, user_input):
        self.history.append({"role": "user", "content": user_input})

    def query(self):
        headers = {
            "Authorization": f"Bearer {ZHIPU_API_KEY}",
            "Content-Type": "application/json"
        }
        data = {
            "model": "glm-4",
            "messages": self.history,
            "temperature": 0.7
        }
        response = requests.post(ZHIPU_API_URL, headers=headers, json=data)
        if response.status_code == 200:
            content = response.json()["choices"][0]["message"]["content"]
            self.history.append({"role": "assistant", "content": content})
            return content
        else:
            raise Exception(f"API 调用失败: {response.text}")

class DebloatGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("FlowSlicer - LLM交互模式")

        self.apk_path = ""
        self.apk_name = ""
        self.llm = LLMDialog()

        tk.Label(root, text="🧠 请输入自然语言降肿需求或对当前 DSL 的修改建议：").pack()
        self.input_text = scrolledtext.ScrolledText(root, height=4)
        self.input_text.pack(padx=10, pady=5, fill=tk.BOTH, expand=True)

        btn_frame = tk.Frame(root)
        btn_frame.pack(pady=5)

        self.query_btn = tk.Button(btn_frame, text="生成/修改 DSL", command=self.generate_dsl)
        self.query_btn.grid(row=0, column=0, padx=5)

        self.upload_btn = tk.Button(btn_frame, text="上传 APK", command=self.upload_apk)
        self.upload_btn.grid(row=0, column=1, padx=5)

        self.reset_btn = tk.Button(btn_frame, text="重置 DSL", command=self.reset_dsl)
        self.reset_btn.grid(row=0, column=2, padx=5)

        self.run_btn = tk.Button(btn_frame, text="确认并执行降肿", state=tk.DISABLED, command=self.run_debloat)
        self.run_btn.grid(row=0, column=3, padx=5)

        self.apk_icon_label = tk.Label(btn_frame)
        self.apk_icon_label.grid(row=0, column=4, padx=5)

        self.apk_name_label = tk.Label(root, text="", fg="gray", font=("Arial", 10, "italic"))
        self.apk_name_label.pack()

        tk.Label(root, text="📜 生成的或自定义的 ADQL 脚本：").pack()
        self.dsl_output = scrolledtext.ScrolledText(root, height=10)
        self.dsl_output.pack(padx=10, pady=5, fill=tk.BOTH, expand=True)

        tk.Label(root, text="📦 状态输出：").pack()
        self.status_output = scrolledtext.ScrolledText(root, height=6)
        self.status_output.pack(padx=10, pady=5, fill=tk.BOTH, expand=True)

    def generate_dsl(self):
        user_text = self.input_text.get("1.0", tk.END).strip()
        if not user_text:
            messagebox.showwarning("提示", "请输入自然语言内容")
            return
        self.llm.add_user_input(user_text)
        self.dsl_output.insert(tk.END, "\n🧠 正在向 LLM 请求...\n")
        try:
            dsl = self.llm.query()
            self.dsl_output.insert(tk.END, "\n--- DSL ---\n" + dsl + "\n")
            self.status_output.insert(tk.END, "✅ DSL 已更新，请确认是否满足要求\n")
            self.check_ready()
        except Exception as e:
            messagebox.showerror("错误", str(e))

    def upload_apk(self):
        path = filedialog.askopenfilename(filetypes=[("APK 文件", "*.apk")])
        if path:
            self.apk_name = os.path.splitext(os.path.basename(path))[0]
            self.apk_path = os.path.join(TARGET_DIR, f"{self.apk_name}.apk")
            try:
                os.makedirs(TARGET_DIR, exist_ok=True)
                shutil.copy(path, self.apk_path)
                self.status_output.insert(tk.END, f"✅ APK 已复制到目标路径：{self.apk_path}\n")

                icon_path = "android_icon.png"
                if os.path.exists(icon_path):
                    from PIL import Image, ImageTk
                    img = Image.open(icon_path).resize((24, 24))
                    self.apk_icon = ImageTk.PhotoImage(img)
                    self.apk_icon_label.config(image=self.apk_icon)
                else:
                    self.apk_icon_label.config(text="📱")
                self.apk_name_label.config(text=f"已上传 APK: {self.apk_name}.apk")

                self.check_ready()
            except Exception as e:
                messagebox.showerror("复制失败", f"无法复制 APK 文件: {e}")

    def reset_dsl(self):
        self.dsl_output.delete("1.0", tk.END)
        self.status_output.insert(tk.END, "🔄 DSL 已重置，请手动输入或重新生成。\n")
        self.run_btn.config(state=tk.DISABLED)

    def check_ready(self):
        dsl_content = self.dsl_output.get("1.0", tk.END).strip()
        if self.apk_name and dsl_content:
            self.run_btn.config(state=tk.NORMAL)
        elif not dsl_content:
            messagebox.showwarning("提示", "DSL 内容为空，无法执行降肿")
            return

    def run_debloat(self):
        try:
            command = [
                "java", "-jar", "flowslicer-2.0.jar",
                "-n", self.apk_name,
                "-p", self.apk_path,
                "-m", "MatchSliceMode"
            ]
            self.status_output.insert(tk.END, f"\n🚀 正在执行命令：{' '.join(command)}\n")
            result = subprocess.run(command, capture_output=True, text=True)

            if result.returncode == 0:
                self.status_output.insert(tk.END, f"✅ 降肿完成！输出已生成。\n")
            else:
                self.status_output.insert(tk.END, f"❌ 降肿失败：\n{result.stderr}\n")

        except Exception as e:
            messagebox.showerror("降肿错误", str(e))

if __name__ == "__main__":
    root = tk.Tk()
    app = DebloatGUI(root)
    root.mainloop()