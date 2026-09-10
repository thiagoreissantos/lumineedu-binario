-- Dados iniciais de exemplo (metadados dos arquivos).
-- O conteudo binario nao e armazenado no banco de dados; ele esta no disco local.

INSERT INTO tb_arquivo (
    nome_original,
    nome_arquivo,
    caminho_relativo,
    caminho_fisico,
    tamanho_bytes,
    tipo_mime,
    descricao,
    data_criacao,
    data_atualizacao
) VALUES
    ('foto_exemplo.jpg',
     'foto_exemplo.jpg',
     'arquivos/foto_exemplo.jpg',
     '/home/lumineedu/armazenamento/arquivos/foto_exemplo.jpg',
     524288,
     'image/jpeg',
     'Exemplo de arquivo de imagem',
     NOW(),
     NOW()),
    ('documento.png',
     'documento.png',
     'arquivos/documento.png',
     '/home/lumineedu/armazenamento/arquivos/documento.png',
     1048576,
     'image/png',
     'Exemplo de arquivo PNG',
     NOW(),
     NOW());
