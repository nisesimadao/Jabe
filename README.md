# Jabe

Jabe は、Bedrock モードが有効な間だけ Minecraft Java Edition を
Bedrock 互換クライアントおよび LAN ホストとして動作させることを目指す、
クライアント側 Fabric Mod です。

## 現在のマイルストーン

最初の実動バージョンは **Minecraft Java Edition 26.1.2** を対象としています。

- Java 版のマルチプレイ画面に **Bedrock LAN** ボタンを追加
- UDP ポート 19132 に RakNet の Unconnected Ping をブロードキャスト
- Bedrock の Unconnected Pong 広告を解析
- ワールド名、プレイヤー数、ゲームバージョン、アドレスを一覧表示
- 内蔵した ViaFabricPlus/ViaBedrock ランタイムを介して選択したワールドへ接続
- Java/Bedrock の互換モードを明示的な状態として管理
- Windows BDS **1.26.43.1** に対し、検出、ログイン、スポーン、地形読み込み、
  移動・水泳、アイテム同期、ブロックの破壊・設置まで実機テスト済み

Jabe の JAR には、パッチ適用済みの ViaFabricPlus **4.5.5** ランタイムと、
upstream の `update/1.26.40` ブランチをベースにした ViaBedrock Protocol 2168
ビルドが内蔵されています。ViaFabricPlus を別途導入する必要はありません。
Jabe には、BDS 1.26.43.1 に必要な v2168 パケットレイアウト修正が含まれており、
リソースパック交渉、エンティティデータ、認証済み入力、圧縮、
チャンク／サブチャンク転送に対応しています。また、v2168 で認証済み入力の
列挙型名が変更された後も ViaFabricPlus 4.5.5 と連携できるよう、
小さなバイナリ互換エイリアスを維持しています。

2026 Drop 3 の実験的パレットについても、欠落ステートのプレースホルダーを
出さずに処理します。Poplar 系は対応する Pale Oak のステートを保持し、
Straw Bed は向き・部位・使用中状態を Yellow Bed として保持します。
動的な Wool 形状と植生には、色を維持するバニラのフォールバックを使用します。

## ビルド

JDK 25 が必要です。

```powershell
.\gradlew.bat build
```

配布用 Mod は `build/libs/jabe-0.1.0.jar` に生成されます。

隣接する `../MineAgent/build/libs/mineagent-0.2.0.jar` が存在する場合、
Loom は開発実行時に `runtimeOnly` 依存関係として追加します。
MineAgent は Jabe の成果物には内蔵されません。`.mcp.json` は MineAgent の
ローカル MCP ブリッジを参照しており、デスクトップ操作を使わずに
起動中のクライアント UI を検査・操作できます。

`vendor/runtime/ViaFabricPlus-4.5.5.jar` と `vendor/ViaBedrock` は
ビルド入力です。`prepareBundledBedrockRuntime` は ViaFabricPlus 内の古い
ViaBedrock JAR を置き換え、パッチ済みランタイムを Jabe の成果物に内蔵します。
ソースと GPL ライセンスについては `THIRD_PARTY_NOTICES.md` を参照してください。

## アーキテクチャ

- `network/BedrockLanDiscovery`: 独立した RakNet LAN 検出トランスポート
- `network/BedrockLanWorld`: 正規化された Bedrock 広告データ
- `network/BedrockSessionConnector`: 任意利用の ViaFabricPlus/ViaBedrock アダプター
- `compat/CompatibilityState`: Java/Bedrock モードの明示的な境界
- `screen/BedrockLanScreen`: 検出および接続 UI

ローカルで所有している `E:\Coding\MCBERecomp` 内の Google Play APK は、
挙動比較のために使用する場合があります。このリポジトリには Minecraft の
バイナリ、アセット、認証コード、DRM 関連データ、抽出した著作物を一切コピーしません。

## 今後のマイルストーン

1. 現在バニラの表示へフォールバックしている実験的な 1.26.43 コンテンツについて、
   アイテム、モデル、テクスチャ、当たり判定形状の再現度を向上する
2. Java 統合サーバー向けの実際の Bedrock LAN ホストブリッジを追加する
   （RakNet/Bedrock サーバー経路が完成するまで、偽の LAN ワールドは広告しない）
3. 所有している Bedrock 1.26.40.5 Android 版と、同一 LAN 上の
   Nintendo Switch に対して動作確認する
