const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read meals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("meals").get());
});

test("Authenticated user: cannot read another user's meal", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("meals").doc("meal_1").set({
      id: "meal_1",
      userId: ALICE_UID,
      name: "Salad",
      mealType: "lunch",
      calories: 350,
      protein_g: 20,
      carbs_g: 30,
      fat_g: 10,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("meals").doc("meal_1").get());
});

test("Authenticated user: can create and read own meal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const mealRef = aliceDb.collection("users").doc(ALICE_UID).collection("meals").doc("meal_1");
  await assertSucceeds(
    mealRef.set({
      id: "meal_1",
      userId: ALICE_UID,
      name: "Chicken Bowl",
      mealType: "dinner",
      calories: 600,
      protein_g: 45,
      carbs_g: 50,
      fat_g: 15,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(mealRef.get());
});

test("Authenticated user: can set and read own goals", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("current");
  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      calories: 2200,
      protein_g: 130,
      goalType: "maintain",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(goalRef.get());
});
