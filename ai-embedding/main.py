from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from FlagEmbedding import BGEM3FlagModel
import os

app = FastAPI()

# 🔥 关键修改：指向你实际的模型缓存目录（替换成自己的快照ID）
LOCAL_MODEL_PATH = "C:/Users/24484/.cache/huggingface/hub/models--BAAI--bge-m3/snapshots/5617a9f61b028005a4858fdac845db406aefb181"

# 验证路径是否存在
if not os.path.exists(LOCAL_MODEL_PATH):
    raise FileNotFoundError(f"模型目录不存在：{LOCAL_MODEL_PATH}")

# 加载本地模型（添加参数确保从本地加载）
model = BGEM3FlagModel(
    LOCAL_MODEL_PATH,
    use_fp16=True,
    # 禁用网络下载，强制使用本地文件
    trust_remote_code=True,
    cache_dir=os.path.dirname(LOCAL_MODEL_PATH)
)

class EmbedRequest(BaseModel):
    text: str

class EmbedResponse(BaseModel):
    vector: list[float]

@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    try:
        # 生成嵌入向量
        outputs = model.encode(
            [req.text],
            batch_size=1,
            max_length=1024,
            return_dense=True,
            return_sparse=False,
            return_colbert_vecs=False,
        )
        dense_vec = outputs["dense_vecs"][0]
        return EmbedResponse(vector=dense_vec.tolist())
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"生成向量失败：{str(e)}")

# 测试接口
@app.get("/")
def root():
    return {"message": "BGE-M3 嵌入服务已启动", "model_path": LOCAL_MODEL_PATH}