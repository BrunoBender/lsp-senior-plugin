/*
 * Regra de exemplo - Linguagem Senior de Programacao (LSP)
 * Calcula a bonificacao dos colaboradores ativos e notifica o RH.
 *
 * Demonstra: tipos, booleanos, controle de fluxo, operadores,
 * cursores SQL, transacoes, funcoes utilitarias e envio de e-mail.
 */

@ ----------------------------------------------------------------------
@ Parametros e variaveis de trabalho
@ ----------------------------------------------------------------------
definir numero codEmpresa;
definir numero codColaborador;
definir alfa   nomeColaborador;
definir numero salarioBase;
definir numero percentualBonus;
definir numero valorBonus;
definir numero totalProcessados;
definir numero totalPago;
definir alfa   corpoEmail;
definir booleano possuiPendencia;

inicio
    codEmpresa       = EmpAtu;
    percentualBonus  = 10;
    totalProcessados = 0;
    totalPago        = 0;
    corpoEmail       = "Relatorio de bonificacao - " + NomEmp;

    @ Abre a transacao para garantir consistencia dos lancamentos
    IniciarTransacao();

    @ Seleciona os colaboradores ativos da empresa corrente
    SQL_Criar();
    SQL_DefinirComando("SELECT NumCad, NomFun, VlrSal FROM R034FUN " +
                       "WHERE NumEmp = :empresa E SitAfa = 0");
    SQL_DefinirInteiro("empresa", codEmpresa);
    SQL_AbrirCursor();

    enquanto nao SQL_EOF()
    {
        codColaborador  = SQL_RetornarInteiro("NumCad");
        nomeColaborador = SQL_RetornarAlfa("NomFun");
        salarioBase     = SQL_RetornarFlutuante("VlrSal");

        @ Bonus maior para salarios mais baixos (regra progressiva)
        se salarioBase < 3000
        {
            valorBonus = Arredonda(salarioBase * (percentualBonus + 5) / 100, 2);
        }
        senao
        {
            valorBonus = Arredonda(salarioBase * percentualBonus / 100, 2);
        }

        @ Ignora valores invalidos
        se valorBonus <= 0 ou EstaNulo(valorBonus)
        {
            possuiPendencia = cVerdadeiro;
            SQL_Proximo();
            continue;
        }

        possuiPendencia  = cFalso;
        totalProcessados = totalProcessados + 1;
        totalPago        = totalPago + valorBonus;

        corpoEmail = corpoEmail + Concatena("\n", IntParaAlfa(codColaborador),
                     " - ", nomeColaborador, ": R$ ", IntParaAlfa(valorBonus));

        SQL_Proximo();
    }

    SQL_FecharCursor();
    SQL_Destruir();

    @ Confirma ou desfaz conforme houve pendencias
    se totalProcessados > 0 e nao possuiPendencia
    {
        FinalizarTransacao();
        EnviarEmail(NomUsu, "Bonificacao processada", corpoEmail);
        Mensagem("Sucesso", IntParaAlfa(totalProcessados) +
                 " colaboradores processados.");
    }
    senao
    {
        DesfazerTransacao();
        Mensagem("Atencao", "Nenhum colaborador processado em " + NomEmp);
    }
fim
