# 印刷厂 · 印版与工单

厂里的台账：**印刷机**、**印版**、**纸张**、**印刷工单**。

两个业务重点：
- **工单按工序走**：待印 → 印刷中 → 已完成，跳步和回退都会被拦；开单前会看**指定的纸够不够**、
  **指定的印版有没有作废**。纸张库存按克重折算，罩不住这批份数就不让开。
- **多条件叠加查询**：客户、状态、用纸、交期区间任意组合，条件给了才拼进 SQL（见 `spec/PrintJobSpecs`），
  页面顶上用条件胶囊把当前生效的条件摆出来。

## 技术栈

- 后端：Spring Boot 3.3 / Java 17、Spring Data JPA（**工单查询用 Specification 动态拼条件**）、MySQL 8、Redis 7
- 前端：Vue 3（Composition API + **一个十几行的自实现事件总线** `utils/bus.js`，
  改完数据广播 `data-changed`）+ Element Plus + Vite
- 一键起：`./start.sh`

## 业务模块

1. **印刷机**（`press`）—— 编号名称型号、机长、运行/停机/封存（封存前要求先卸版）
2. **印版**（`plate`）—— 编号版面尺寸、装在哪个机器上、在用/已磨损/已作废
3. **纸张**（`paper`）—— 纸号纸名克重、库存与预警线、充足/紧张/缺货
4. **印刷工单**（`print_job`）—— 客户、用纸、印版、份数、交期、三态流转

## 本地跑起来

| | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8236/ |
| 后端接口 | http://127.0.0.1:8336/api/jobs |
| MySQL | 127.0.0.1:3536（库 `print_shop`） |
| Redis | 127.0.0.1:6536 |

容器名统一是 `claude-qd-306-{mysql,redis,backend,frontend}`。

```bash
./start.sh              # 起容器
docker compose ps       # 看状态
docker compose down -v  # 停掉并清数据
```
