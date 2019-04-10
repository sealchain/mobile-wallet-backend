CREATE DOMAIN public.address AS text;

ALTER DOMAIN public.address OWNER TO postgres;

CREATE TABLE public.bestblock (
    best_block_num bigint
);


CREATE TABLE public.pending_txs (
    hash text NOT NULL
);

CREATE TABLE public.tx_addresses (
    tx_hash text,
    address public.address
    -- currency integer 这里有没有必要新增币种字段？
);


CREATE TABLE public.txs (
    hash text NOT NULL,
    inputs_address text[],
    inputs_amount bigint[],
    outputs_address text[],
    outputs_amount bigint[],
    -- 下面4列-新增黄金币的输入输出
    goldInputs_address text[],
    goldInputs_amount bigint[],
    goldOutputs_address text[],
    goldOutputs_amount bigint[],
    -- 下面4列-新增稳定币的输入输出
    dollarInputs_address text[],
    dollarInputs_amount bigint[],
    dollarOutputs_address text[],
    dollarOutputs_amount bigint[],
    block_num bigint,
    "time" timestamp with time zone
);


CREATE TABLE public.utxos (
    utxo_id text NOT NULL,
    tx_hash text,
    tx_index integer,
    receiver text,
    currency integer,  -- 新增币种字段(0 - seal币, 1 - 黄金币, 2 - 稳定币)
    amount bigint
);


-- ALTER TABLE public.txs OWNER TO postgres;


