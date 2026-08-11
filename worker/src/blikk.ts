/**
 * Minimal Blikk Ecom client — payment initiation on the RÍR (reikningur í
 * reikning) push rail, no cards.
 *
 * Ported from `flue-lab/packages/blikk`, where the response shapes were
 * confirmed against production on 2026-07-16 (flue-lab#28). Kept to the two
 * calls the In-App experience needs (create + status); refund/cancel live in
 * the flue-lab package and can be ported when a real refund flow lands.
 *
 * Auth is the `API-Key` header. Casing differs from the POS and Payment
 * Channel APIs (`Api-Key`) — HTTP headers are case-insensitive, but the
 * flue-lab spelling is the one proven in production, so keep it.
 */

export const BLIKK_ECOM_BASE_URL = "https://api.blikk.tech/ecom";

/** Documented lifecycle: DRAFT → PENDING → SCA_REQUIRED → SCA_COMPLETE → one
 * of the four terminal states. `string` passthrough so an undocumented status
 * never throws. */
export type BlikkPaymentStatus =
  | "DRAFT"
  | "PENDING"
  | "SCA_REQUIRED"
  | "SCA_COMPLETE"
  | "SUCCESS"
  | "ERROR"
  | "REJECTED"
  | "CANCELLED";

const TERMINAL_STATUSES: ReadonlySet<string> = new Set([
  "SUCCESS",
  "ERROR",
  "REJECTED",
  "CANCELLED",
]);

export function isTerminalStatus(status: string): boolean {
  return TERMINAL_STATUSES.has(status);
}

export function isPaidStatus(status: string): boolean {
  return status === "SUCCESS";
}

export interface CreatePaymentInput {
  /** Whole ISK — the currency has no subunit. */
  amount: number;
  currency?: string;
  /** Where the payer is sent after SCA. Blikk appends `?paymentId=<id>`, and
   * a custom app scheme works — verified live on stage 2026-07-14. */
  partnerRedirectUrl?: string;
  /** Unsigned webhook target. Omitted by the test surface below on purpose. */
  callbackUrl?: string;
  /** Idempotency: while a non-terminal payment with the same reference and
   * amount exists, the API returns it instead of creating a second one. */
  sourceReferenceId?: string;
  source?: string;
  /** Payer kennitala / phone. Omit BOTH for an anonymous payment — the payer
   * then identifies on Blikk's own hosted page and no personal data passes
   * through this worker at all. */
  debtorExternalId?: string;
  debtorPhoneNo?: string;
  /** Unix seconds. The API default is only 120 s (300 s with onboarding). */
  expiresAt?: number;
}

/**
 * Direct Debtor create — a payment for a payer we already hold details for,
 * so no Blikk onboarding page stands between them and their banking app.
 * This is the path agreed with Blikk at the 2026-07-15 meeting for the Rósa
 * Parks top-up (`blikk-integration/rosa-parks-topup-flow.md`).
 *
 * Enabled per sales channel — a channel without it answers 403.
 */
export interface DirectDebtorPaymentInput {
  amount: number;
  /** Payer kennitala. Required on this endpoint, unlike the anonymous path. */
  debtorExternalId: string;
  /** Payer account, 12 digits, no separators: banki(4) + höfuðbók(2) + nr(6). */
  debtorBban: string;
  debtorName: string;
  currency?: string;
  partnerRedirectUrl?: string;
  callbackUrl?: string;
  sourceReferenceId?: string;
  source?: string;
  debtorPhoneNo?: string;
  /** Returned by validate-bban when the account is a corporate one — must be
   * used instead of debtorExternalId in that case. */
  debtorCorpExternalId?: string;
  expiresAt?: number;
}

/** Zero-funds probe: does this account exist, and does it belong to this
 * kennitala? Moves no money and creates no payment, so it is the right
 * pre-flight before a Direct Debtor create. */
export interface ValidateBbanResult {
  isValid: boolean;
  isCorporateAccount: boolean;
  iban: string;
  bban: string;
  errorMessage: string;
  /** When present, use this instead of the kennitala that was sent. */
  newExternalId?: string;
  newIban?: string;
}

export interface CreatePaymentResult {
  id: string;
  status: BlikkPaymentStatus;
  /** Hosted payment page. Empty for banks using back-channel push SCA (e.g.
   * Íslandsbanki) — then there is nothing to open and the payer gets a push
   * from their own banking app instead. */
  scaRedirectUrl: string;
  message: string;
  partnerRedirectUrl: string;
}

export interface GetPaymentResult {
  id: string;
  status: BlikkPaymentStatus;
  scaRedirectUrl: string;
  message: string;
}

export class BlikkApiError extends Error {
  readonly status: number;
  readonly body: string;

  constructor(status: number, body: string, url: string) {
    super(`Blikk API ${status} on ${url}`);
    this.name = "BlikkApiError";
    this.status = status;
    this.body = body;
  }
}

export interface BlikkClientOptions {
  /** Per-sales-channel key from merchants.blikk.tech. Inject from the
   * environment at the call site — never read globals in here. */
  apiKey: string;
  baseUrl?: string;
}

export class BlikkClient {
  private readonly apiKey: string;
  private readonly baseUrl: string;

  constructor(options: BlikkClientOptions) {
    if (!options.apiKey) throw new Error("BlikkClient: apiKey is required");
    this.apiKey = options.apiKey;
    this.baseUrl = (options.baseUrl ?? BLIKK_ECOM_BASE_URL).replace(/\/$/, "");
  }

  private async request<T>(method: "GET" | "POST", path: string, body?: unknown): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const response = await fetch(url, {
      method,
      headers: {
        "API-Key": this.apiKey,
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    const text = await response.text();
    if (!response.ok) throw new BlikkApiError(response.status, text, url);
    // Thin client: the shape is the OpenAPI contract. Runtime schema
    // validation is deliberately out of scope.
    return JSON.parse(text) as T;
  }

  createPayment(input: CreatePaymentInput): Promise<CreatePaymentResult> {
    return this.request<CreatePaymentResult>("POST", "/v3/payments", input);
  }

  createDirectDebtorPayment(input: DirectDebtorPaymentInput): Promise<CreatePaymentResult> {
    return this.request<CreatePaymentResult>("POST", "/v3/payments/direct-debtor", input);
  }

  validateBban(bban: string, externalId: string): Promise<ValidateBbanResult> {
    return this.request<ValidateBbanResult>("POST", "/account/validate-bban", { bban, externalId });
  }

  getPayment(id: string): Promise<GetPaymentResult> {
    return this.request<GetPaymentResult>("GET", `/v3/payments/${encodeURIComponent(id)}`);
  }
}
