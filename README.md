# DimDim Pedidos

Aplicação de gestão de pedidos em Java para o checkpoint de Aplicações e Banco em Nuvem.

Aplicação publicada: [DimDim Pedidos](https://dimdim-pedidos-261002-ce05.azurewebsites.net/pedidos).

## Leia primeiro

- [READM.MD — aplicação de ponta a ponta](READM.MD).
- [Guia de execução — requisitos, passos e caminhos](docs/guia-execucao.md).
- [How To — compilação e implantação](docs/how-to.md).
- [Roteiro de apresentação](docs/roteiro-apresentacao.md).
- [PDF de entrega](output/pdf/CP5-DEVOPS_webapp.pdf).

Grupo CP5-DEVOPS: Gabriel Sbrana Campos (RM565849), Thiago Rodrigues da Mota (RM563650) e Moisés Waidemann Molinillo (RM563719).

Repositório de entrega: https://github.com/sbranaw/CP5-DEVOPS.

Cada etapa termina com a apresentação do resultado. A próxima etapa começa somente após autorização do usuário.

## Escopo adotado

- Aplicação com telas em português, sem login nesta versão inicial.
- Backend em Java com Spring Boot, Spring Data JPA e páginas com Thymeleaf.
- Persistência no Azure SQL Database.
- Criação dos recursos e deploy pelo Azure CLI.
- Integração com Application Insights.
- Sem cadastro independente de produtos: cada item guarda a descrição e o preço praticado no pedido.

A interface com telas e a ausência de login são escolhas iniciais propostas na conversa e podem ser revistas antes da implementação. Os nomes e RMs do grupo serão preenchidos na documentação final.

## Documentos

- `docs/etapas.md`: etapas e critérios de conclusão.
- `docs/modelo-e-regras.md`: dados, relacionamento e regras de negócio.
- `database/001-create-tables.sql`: DDL inicial para Azure SQL.
- `docs/execucao-local.md`: como compilar, testar e configurar o banco.
- `docs/resultado-etapa-2.md`: mudanças e evidências desta etapa.
- `docs/resultado-etapa-3.md`: telas, verificações e limitações da etapa 3.
- `docs/implantacao-azure.md`: guia e comandos para provisionar, publicar e verificar.
- `docs/resultado-etapa-4.md`: recursos e evidências da implantação real.
- `docs/application-insights.md`: configuração e consultas do monitoramento.
- `docs/resultado-etapa-5.md`: resultados e evidências da telemetria real.

## Estrutura prevista

O projeto usa Java 21, Spring Boot 4.0.8 e Maven Wrapper 3.9.15. O código fica em `src/main/java`, as configurações e páginas em `src/main/resources` e os testes em `src/test/java`. Os comandos de infraestrutura ficam em `infra`, e os guias em `docs`.

Para compilar e executar os testes locais no Windows, abra um terminal nesta pasta e execute `./mvnw.cmd verify`. Os testes locais não precisam de banco externo nem de credenciais. Os dois testes Azure SQL são opcionais e executam somente com DIMDIM_AZURE_TEST=true e conexão configurada. A primeira execução do Wrapper em outro computador requer internet para baixar Maven e dependências.

Para experimentar as telas, execute `java -jar target/dimdim-pedidos-0.1.0.jar --spring.profiles.active=demo` e abra http://127.0.0.1:8080/pedidos. Esse modo utiliza dados temporários locais. Para Azure SQL, consulte o guia de execução e mantenha o perfil padrão.

Não colocar senhas, tokens ou strings de conexão com credenciais no repositório. As configurações de acesso serão fornecidas pelo ambiente de execução.

## Referência

Requisitos extraídos de `2o Checkpoint 2o Semestre - Aplicativos e Banco em Nuvem 1.pptx`, preservado em `sources` no diretório do projeto ChatGPT.
