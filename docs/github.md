# GitHub — entrega do projeto

Destino autorizado: https://github.com/sbranaw/CP5-DEVOPS.

Inclua src, pom.xml, mvnw, mvnw.cmd, .mvn, .gitignore, README.md, READM.MD, database, infra, monitoring e docs.

README.md é a entrada do GitHub; READM.MD contém a explicação completa solicitada. Os dois nomes coexistem no Windows pois diferem pela ausência da letra E.

Exclua target, .local, credenciais, logs e arquivos .env. Revise o conteúdo preparado antes do commit. Preserve o histórico e arquivos existentes do destino; não use push forçado.

Para uma cópia já clonada:
```powershell
git status --short
git add .
git diff --cached --stat
git commit -m 'Documenta entrega DimDim Java, Azure SQL e monitoramento'
git push
```
Autentique pelo GitHub sem inserir tokens na URL. Confirme que o professor tem acesso ao repositório e confira os links após o envio.
